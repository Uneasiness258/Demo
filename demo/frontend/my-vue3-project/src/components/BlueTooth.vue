<script setup>
import { ref, onBeforeUnmount } from "vue";
import { ElMessage, ElLoading } from "element-plus";
import { throttle } from 'lodash-es';

const SERVICE_UUID = "0000ffe5-0000-1000-8000-00805f9a34fb";
const READ_CHAR_UUID = "0000ffe4-0000-1000-8000-00805f9a34fb";
const WRITE_CHAR_UUID = "0000ffe9-0000-1000-8000-00805f9a34fb";

// 状态管理
const devices = ref([]);
const isConnecting = ref(false);
const isCollecting = ref(false);

// 优化后的数据结构 - 使用环形缓冲区
const bufferSize = 100000; // 最多存储100000条数据
const imuDataArray = ref({
  buffer: Array(bufferSize),
  head: 0,
  tail: 0,
  count: 0,
  pageSize: 100,
  currentPage: 0,
  get visibleData() {
    const start = (this.currentPage * this.pageSize) % bufferSize;
    const end = Math.min(start + this.pageSize, this.count);
    const result = [];

    for (let i = 0; i < end; i++) {
      const index = (this.head - 1 - i + bufferSize) % bufferSize;
      if (this.buffer[index]) {
        result.push(this.buffer[index]);
      }
    }

    return result;
  }
});

// 组件卸载前清理
onBeforeUnmount(() => {
  stopAllDevices();
});

// 连接单个蓝牙设备
async function connectSingleDevice() {
  if (devices.value.length >= 6) {
    ElMessage.warning("已连接6个设备，请开始采集数据！");
    return;
  }

  try {
    isConnecting.value = true;


    const device = await navigator.bluetooth.requestDevice({
      filters: [{ namePrefix: "WT" }],
      optionalServices: [SERVICE_UUID],
    });

    device.addEventListener('gattserverdisconnected', () => handleDisconnect(device.id));

    const server = await device.gatt.connect();
    const service = await server.getPrimaryService(SERVICE_UUID);

    const [notifyChar, writeChar] = await Promise.all([
      service.getCharacteristic(READ_CHAR_UUID),
      service.getCharacteristic(WRITE_CHAR_UUID)
    ]);

    // 初始化设备
    const deviceObj = {
      id: device.id,
      name: device.name || `设备${devices.value.length + 1}`,
      device,
      server,
      notifyChar,
      writeChar,
      connected: true,
      lastData: null,
      tempBytes: []
    };

    devices.value.push(deviceObj);

    // 启动通知
    await notifyChar.startNotifications();

    // 使用节流处理高频事件
    const throttledHandler = throttle((event) => {
      handleData(event, device.id);
    }, 50); // 50ms节流

    notifyChar.addEventListener('characteristicvaluechanged', throttledHandler);

    // 设置设备输出默认数据包
    //await sendCommandWithDelay(deviceObj, [0xFF, 0xAA, 0x96, 0x00, 0x00]);

    ElMessage.success(`${device.name || '设备'}已连接！`);

  } catch (error) {
    console.error('连接失败:', error);
    ElMessage.error(`连接失败: ${error.message}`);
  } finally {
    isConnecting.value = false;
    ElLoading.service().close();
  }
}



// 处理蓝牙数据 - 优化版本
function handleData(event, deviceId) {
  if (!isCollecting.value) return;

  const value = event.target.value;
  const bytes = new Uint8Array(value.buffer);
  const device = devices.value.find(d => d.id === deviceId);
  if (!device) return;

  // 处理字节流
  for (const byte of bytes) {
    device.tempBytes.push(byte);

    // 检查包头
    if (device.tempBytes.length === 1 && device.tempBytes[0] !== 0x55) {
      device.tempBytes.shift();
      continue;
    }

    if (device.tempBytes.length === 2 && device.tempBytes[1] !== 0x61) {
      device.tempBytes.shift();
      continue;
    }

    // 完整数据包处理
    if (device.tempBytes.length === 20) {
      const data = processDataPacket(device.tempBytes, device);

      // 使用环形缓冲区存储数据
      const index = imuDataArray.value.tail;
      imuDataArray.value.buffer[index] = data;
      imuDataArray.value.tail = (index + 1) % bufferSize;
      if (imuDataArray.value.count < bufferSize) {
        imuDataArray.value.count++;
      } else {
        imuDataArray.value.head = (imuDataArray.value.head + 1) % bufferSize;
      }

      device.lastData = data;
      device.tempBytes = [];
    }
  }
}

// 处理完整数据包
function processDataPacket(bytes, device) {
  const view = new DataView(new Uint8Array(bytes).buffer);

  const getInt16 = (offset) => {
    const val = view.getInt16(offset, true);
    return val >= 32768 ? val - 65536 : val;
  };

  return {
    timestamp: new Date().toISOString(),
    deviceName: device.name,
    deviceId: device.id,
    AccX: (getInt16(2) / 32768 * 16).toFixed(4),
    AccY: (getInt16(4) / 32768 * 16).toFixed(4),
    AccZ: (getInt16(6) / 32768 * 16).toFixed(4),
    GyroX: (getInt16(8) / 32768 * 2000).toFixed(2),
    GyroY: (getInt16(10) / 32768 * 2000).toFixed(2),
    GyroZ: (getInt16(12) / 32768 * 2000).toFixed(2),
    Roll: (getInt16(14) / 32768 * 180).toFixed(2),
    Pitch: (getInt16(16) / 32768 * 180).toFixed(2),
    Yaw: (getInt16(18) / 32768 * 180).toFixed(2)
  };
}

// 开始数据采集
function startDataCollection() {
  if (devices.value.length < 6) {
    ElMessage.warning("请先连接6个设备！");
    return;
  }

  isCollecting.value = true;
  // 重置缓冲区
  imuDataArray.value.buffer = Array(bufferSize);
  imuDataArray.value.head = 0;
  imuDataArray.value.tail = 0;
  imuDataArray.value.count = 0;
  imuDataArray.value.currentPage = 0;
  ElMessage.success("开始采集数据...");
}

// 停止数据采集
function stopDataCollection() {
  isCollecting.value = false;
  ElMessage.info("已停止数据采集");
}

// 处理设备断开
function handleDisconnect(deviceId) {
  const device = devices.value.find(d => d.id === deviceId);
  if (device) {
    device.connected = false;
    ElMessage.warning(`${device.name || '设备'}已断开连接`);
  }
}

// 断开所有设备
async function stopAllDevices() {
  stopDataCollection();

  const disconnectPromises = devices.value.map(async (dev) => {
    try {
      if (dev.connected) {
        if (dev.notifyChar) {
          try {
            await dev.notifyChar.stopNotifications();
          } catch (error) {
            console.error('停止通知失败:', error);
          }
        }
        if (dev.server) {
          try {
            await dev.server.disconnect();
          } catch (error) {
            console.error('断开连接失败:', error);
          }
        }
      }
    } catch (error) {
      console.error('断开设备时出错:', error);
    }
  });

  await Promise.all(disconnectPromises);
  devices.value = [];
  ElMessage.info("已断开所有设备");
}

// 导出CSV文件
function exportToCSV() {
  if (imuDataArray.value.count === 0) {
    ElMessage.warning("没有数据可以导出");
    return;
  }

  const loading = ElLoading.service({
    lock: true,
    text: '正在生成CSV文件...',
    background: 'rgba(0, 0, 0, 0.7)'
  });

  try {
    // 使用requestIdleCallback避免阻塞UI
    requestIdleCallback(() => {
      const headers = [
        '时间戳', '设备ID', '设备名称',
        'AccX(g)', 'AccY(g)', 'AccZ(g)',
        'GyroX(°/s)', 'GyroY(°/s)', 'GyroZ(°/s)',
        'Roll(°)', 'Pitch(°)', 'Yaw(°)'
      ].join(',') + '\n';

      // 收集所有数据
      const allData = [];
      for (let i = 0; i < imuDataArray.value.count; i++) {
        const index = (imuDataArray.value.head + i) % bufferSize;
        if (imuDataArray.value.buffer[index]) {
          allData.push(imuDataArray.value.buffer[index]);
        }
      }

      // 分批处理数据，避免长时间阻塞
      const batchSize = 1000;
      let csvContent = "\uFEFF" + headers;

      for (let i = 0; i < allData.length; i += batchSize) {
        const batch = allData.slice(i, i + batchSize);
        csvContent += batch.map(row =>
          [
            `"${row.timestamp}"`, row.deviceId, `"${row.deviceName}"`,
            row.AccX, row.AccY, row.AccZ,
            row.GyroX, row.GyroY, row.GyroZ,
            row.Roll, row.Pitch, row.Yaw
          ].join(',')
        ).join('\n');
      }

      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `imu_data_${new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    });
  } catch (error) {
    console.error('导出CSV失败:', error);
    ElMessage.error('导出CSV失败');
  } finally {
    loading.close();
  }
}

// 清空数据
function clearData() {
  imuDataArray.value.buffer = Array(bufferSize);
  imuDataArray.value.head = 0;
  imuDataArray.value.tail = 0;
  imuDataArray.value.count = 0;
  imuDataArray.value.currentPage = 0;
  ElMessage.success("已清空所有数据");
}

// 切换数据页
function changePage(newPage) {
  imuDataArray.value.currentPage = newPage - 1;
}
</script>

<template>
  <div class="background">
    <div class="content-wrapper">
      <el-container class="app-container">
        <!-- 头部 -->
        <el-header class="app-header">
          <h1>多设备IMU数据采集系统</h1>
          <div class="header-actions">
            <el-button type="primary" @click="connectSingleDevice" :disabled="isConnecting || devices.length >= 6">
              {{ isConnecting ? "正在连接..." : `连接设备 (${devices.length}/6)` }}
            </el-button>

            <el-button-group>
              <el-button type="success" @click="startDataCollection" :disabled="devices.length < 6 || isCollecting">
                开始采集
              </el-button>
              <el-button type="warning" @click="stopDataCollection" :disabled="!isCollecting">
                停止采集
              </el-button>
            </el-button-group>

            <el-button-group>
              <el-button type="info" @click="exportToCSV" :disabled="imuDataArray.count === 0">
                导出CSV
              </el-button>
              <el-button type="danger" @click="clearData" :disabled="imuDataArray.count === 0">
                清空数据
              </el-button>
            </el-button-group>
          </div>
        </el-header>

        <!-- 主体内容 -->
        <el-main class="app-main">
          <el-row :gutter="20">
            <!-- 设备列表 -->
            <el-col :span="6">
              <el-card class="device-list">
                <template #header>
                  <div class="card-header">
                    <span>已连接设备</span>
                    <el-button type="text" @click="stopAllDevices" :disabled="devices.length === 0">
                      断开全部
                    </el-button>
                  </div>
                </template>

                <el-empty v-if="devices.length === 0" description="未连接设备" />

                <div v-else class="device-item" v-for="dev in devices" :key="dev.id">
                  <div class="device-info">
                    <el-tag :type="dev.connected ? 'success' : 'danger'" size="small">
                      {{ dev.connected ? '在线' : '离线' }}
                    </el-tag>
                    <span>{{ dev.name }}</span>
                  </div>
                  <div class="device-data" v-if="dev.lastData">
                    <div>Acc: {{ dev.lastData.AccX }}, {{ dev.lastData.AccY }}, {{ dev.lastData.AccZ }}</div>
                    <div>Gyro: {{ dev.lastData.GyroX }}, {{ dev.lastData.GyroY }}, {{ dev.lastData.GyroZ }}</div>
                  </div>
                </div>
              </el-card>
            </el-col>

            <!-- 数据表格 -->
            <el-col :span="18">
              <el-card>
                <template #header>
                  <div class="card-header">
                    <span>IMU数据 (实时采集)</span>
                    <span class="data-count">共 {{ imuDataArray.count }} 条数据</span>
                  </div>
                </template>

                <el-table
                  :data="imuDataArray.visibleData"
                  border
                  height="500"
                  style="width: 100%"
                  v-loading="isCollecting"
                  :row-key="row => row.timestamp + row.deviceId"
                >
                  <el-table-column prop="timestamp" label="时间" width="180" />
                  <el-table-column prop="deviceName" label="设备名称" width="120" />

                  <el-table-column label="加速度(g)">
                    <el-table-column prop="AccX" label="X" width="90" />
                    <el-table-column prop="AccY" label="Y" width="90" />
                    <el-table-column prop="AccZ" label="Z" width="90" />
                  </el-table-column>

                  <el-table-column label="角速度(°/s)">
                    <el-table-column prop="GyroX" label="X" width="90" />
                    <el-table-column prop="GyroY" label="Y" width="90" />
                    <el-table-column prop="GyroZ" label="Z" width="90" />
                  </el-table-column>

                  <el-table-column label="角度(°)">
                    <el-table-column prop="Roll" label="Roll" width="90" />
                    <el-table-column prop="Pitch" label="Pitch" width="90" />
                    <el-table-column prop="Yaw" label="Yaw" width="90" />
                  </el-table-column>
                </el-table>

                <el-pagination
                  v-if="imuDataArray.count > imuDataArray.pageSize"
                  @current-change="changePage"
                  :current-page="imuDataArray.currentPage + 1"
                  :page-size="imuDataArray.pageSize"
                  layout="prev, pager, next"
                  :total="imuDataArray.count"
                  class="pagination"
                />
              </el-card>
            </el-col>
          </el-row>
        </el-main>
      </el-container>
    </div>
  </div>
</template>


<style scoped>
.background {
  width: 100vw;
  height: 100vh;
  background: url('@/assets/fig1.png') no-repeat center center;
  background-size: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  background-size: cover;
  opacity: 0.5;
  z-index: 1;
}

.content-wrapper {
  width: 95%;
  height: 95%;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 2;
}

.app-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.app-header {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: rgba(255, 255, 255, 0.5); /* 半透明白 */
  color: #000; /* 黑色文字 */
  padding: 20px;
  font-size: 24px; /* 适中字体大小 */
  font-weight: bold;
  text-align: center;
  border-bottom: 1px solid #ddd;
  backdrop-filter: blur(5px);
}

.header-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}

.app-main {
  padding: 20px;
  height: calc(100% - 120px); /* 头部高度调大后，这里调整 */
  overflow: auto;
}

.device-list {
  height: 100%;
  min-height: 500px;
}

.device-item {
  padding: 12px;
  border-bottom: 1px solid #eee;
  margin-bottom: 8px;
}

.device-item:last-child {
  border-bottom: none;
}

.device-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-weight: bold;
}

.device-data {
  font-size: 12px;
  color: #666;
  line-height: 1.5;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.data-count {
  font-size: 14px;
  color: #666;
}

.pagination {
  margin-top: 15px;
  justify-content: flex-end;
}
</style>