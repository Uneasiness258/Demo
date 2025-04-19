<script setup>
import { ref, onBeforeUnmount } from "vue";
import { ElMessage } from "element-plus";

const SERVICE_UUID = "0000ffe5-0000-1000-8000-00805f9a34fb";
const READ_CHAR_UUID = "0000ffe4-0000-1000-8000-00805f9a34fb";
const WRITE_CHAR_UUID = "0000ffe9-0000-1000-8000-00805f9a34fb";

// 状态管理
const devices = ref([]);
const imuDataArray = ref([]);
const isConnecting = ref(false);
const isCollecting = ref(false);

// 临时字节存储
const tempBytesMap = ref({});

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
      lastData: {
        timestamp: null,
        AccX: null, AccY: null, AccZ: null,
        GyroX: null, GyroY: null, GyroZ: null,
        Roll: null, Pitch: null, Yaw: null,
        Q0: null, Q1: null, Q2: null, Q3: null
      },
      tempBytes: [],
      requestInterval: null
    };

    devices.value.push(deviceObj);
    tempBytesMap.value[device.id] = [];

    // 启动通知
    await notifyChar.startNotifications();
    notifyChar.addEventListener('characteristicvaluechanged', (event) => {
      handleData(event, device.id);
    });

    // 设置设备输出默认数据包
    await sendCommandWithDelay(deviceObj, [0xFF, 0xAA, 0x96, 0x00, 0x00]);
    
    // 启动定时请求四元数 (每0.5秒)
    deviceObj.requestInterval = setInterval(() => {
      requestQuaternion(deviceObj);
    }, 500);

    ElMessage.success(`${device.name || '设备'}已连接！`);
    
  } catch (error) {
    ElMessage.error(`连接失败: ${error.message}`);
  } finally {
    isConnecting.value = false;
  }
}

// 带延迟的发送命令
async function sendCommandWithDelay(device, commandArray, delay = 100) {
  return new Promise((resolve) => {
    setTimeout(async () => {
      try {
        await device.writeChar.writeValue(new Uint8Array(commandArray));
        resolve(true);
      } catch (error) {
        console.error(`向设备${device.id}发送命令失败:`, error);
        resolve(false);
      }
    }, delay);
  });
}

// 请求四元数数据
async function requestQuaternion(device) {
  if (!device.connected) return;
  
  // FF AA 27 51 00
  await sendCommandWithDelay(device, [0xFF, 0xAA, 0x27, 0x51, 0x00]);
}

// 处理蓝牙数据
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
    
    if (device.tempBytes.length === 2 && (device.tempBytes[1] !== 0x61 && device.tempBytes[1] !== 0x71)) {
      device.tempBytes.shift();
      continue;
    }
    
    // 完整数据包处理
    if (device.tempBytes.length === 20) {
      processDataPacket(device.tempBytes, device);
      device.tempBytes = [];
    }
  }
}

// 处理完整数据包
function processDataPacket(bytes, device) {
  const getInt16 = (offset) => {
    const val = (bytes[offset + 1] << 8) | bytes[offset];
    return val >= 32768 ? val - 65536 : val;
  };

  const timestamp = new Date().toISOString();
  
  // 默认数据包 (0x55 0x61)
  if (bytes[1] === 0x61) {
    device.lastData = {
      ...device.lastData,
      timestamp,
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
  // 四元数数据包 (0x55 0x71 0x51 0x00)
  else if (bytes[1] === 0x71 && bytes[2] === 0x51 && bytes[3] === 0x00) {
    const quaternionData = {
      Q0: (getInt16(4) / 32768).toFixed(6),
      Q1: (getInt16(6) / 32768).toFixed(6),
      Q2: (getInt16(8) / 32768).toFixed(6),
      Q3: (getInt16(10) / 32768).toFixed(6)
    };
    
    // 合并最新IMU数据和四元数数据
    const combinedData = {
      ...device.lastData,
      ...quaternionData,
      deviceName: device.name,
      deviceId: device.id
    };
    
    // 确保有时间戳和基本数据
    if (combinedData.timestamp) {
      imuDataArray.value.push(combinedData);
      
      // 限制数据量
      if (imuDataArray.value.length > 1000) {
        imuDataArray.value = imuDataArray.value.slice(-500);
      }
    }
  }
}

// 开始数据采集
function startDataCollection() {
  if (devices.value.length < 6) {
    ElMessage.warning("请先连接6个设备！");
    return;
  }

  isCollecting.value = true;
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
    if (device.requestInterval) {
      clearInterval(device.requestInterval);
      device.requestInterval = null;
    }
    ElMessage.warning(`${device.name || '设备'}已断开连接`);
  }
}

// 断开所有设备
async function stopAllDevices() {
  stopDataCollection();
  
  for (const dev of devices.value) {
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
      if (dev.requestInterval) {
        clearInterval(dev.requestInterval);
        dev.requestInterval = null;
      }
    } catch (error) {
      console.error('断开设备时出错:', error);
    }
  }
  
  devices.value = [];
  ElMessage.info("已断开所有设备");
}

// 导出CSV文件
function exportToCSV() {
  if (imuDataArray.value.length === 0) {
    ElMessage.warning("没有数据可以导出");
    return;
  }

  const headers = [
    '时间戳', '设备ID', '设备名称',
    'AccX(g)', 'AccY(g)', 'AccZ(g)',
    'GyroX(°/s)', 'GyroY(°/s)', 'GyroZ(°/s)',
    'Roll(°)', 'Pitch(°)', 'Yaw(°)',
    'Q0', 'Q1', 'Q2', 'Q3'
  ].join(',') + '\n';

  const rows = imuDataArray.value.map(row => 
    [
      `"${row.timestamp}"`, row.deviceId, `"${row.deviceName}"`,
      row.AccX, row.AccY, row.AccZ,
      row.GyroX, row.GyroY, row.GyroZ,
      row.Roll, row.Pitch, row.Yaw,
      row.Q0, row.Q1, row.Q2, row.Q3
    ].join(',')
  ).join('\n');

  const csvContent = "\uFEFF" + headers + rows;
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `imu_data_${new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')}.csv`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

// 清空数据
function clearData() {
  imuDataArray.value = [];
  ElMessage.success("已清空所有数据");
}
</script>

<template>
  <!-- 界面部分保持不变，与之前相同 -->
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
          <el-button type="info" @click="exportToCSV" :disabled="imuDataArray.length === 0">
            导出CSV
          </el-button>
          <el-button type="danger" @click="clearData" :disabled="imuDataArray.length === 0">
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
            </div>
          </el-card>
        </el-col>
        
        <!-- 数据表格 -->
        <el-col :span="18">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>IMU数据 (每0.5秒采集一次)</span>
              </div>
            </template>
            
            <el-table
              :data="imuDataArray.slice().reverse().slice(0, 100)"
              border
              height="500"
              style="width: 100%"
              v-loading="isCollecting"
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
              
              <el-table-column label="四元数">
                <el-table-column prop="Q0" label="Q0" width="100" />
                <el-table-column prop="Q1" label="Q1" width="100" />
                <el-table-column prop="Q2" label="Q2" width="100" />
                <el-table-column prop="Q3" label="Q3" width="100" />
              </el-table-column>
            </el-table>
            
            <div class="data-stats">
              共 {{ imuDataArray.length }} 条数据 | 
              最新时间: {{ imuDataArray[imuDataArray.length - 1]?.timestamp || '无' }}
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-main>
  </el-container>
</template>

<style scoped>
/* 样式保持不变 */
.app-container {
  height: 100vh;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #409EFF;
  color: white;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.app-main {
  padding: 20px;
  background-color: #f5f7fa;
}

.device-list {
  height: 100%;
}

.device-item {
  padding: 10px;
  border-bottom: 1px solid #eee;
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.data-stats {
  margin-top: 10px;
  font-size: 12px;
  color: #666;
  text-align: right;
}
</style>