<template>
  <div class="patient-manage">
    <div class="background"></div>

    <div class="content">
      <h2>患者管理</h2>

      <button @click="goBack">返回用户界面</button>

      <div>
        <input v-model="searchQuery" type="text" placeholder="搜索患者姓名..." />
      </div>

      <div>
        <label for="patient">选择患者: </label>
        <select v-model="selectedPatient" id="patient">
          <option v-for="patient in filteredPatients" :key="patient.id" :value="patient.id">
            {{ patient.name }}
          </option>
        </select>
      </div>

      <div v-if="selectedPatient">
        <h3>患者: {{ getPatientName(selectedPatient) }}</h3>
        <button @click="showMeasurementData">查看测量数据</button>
        <button @click="showHistoryData">查看历史数据</button>
        <button @click="measureDataAction">测量数据</button>
      </div>

      <!-- 测量时显示进度条 -->
      <div v-if="progress > 0 && progress < 100" class="progress-container">
        <progress :value="progress" max="100"></progress>
        <div>{{ progress }}%</div>
      </div>

      <div v-if="show3DData">
        <h3>3D测量数据展示</h3>
        <img src="@/assets/3d-measurement.gif" alt="3D Measurement Data" />
      </div>

      <div v-if="historyData.length">
        <h3>历史数据</h3>
        <ul>
          <li v-for="(record, index) in historyData" :key="index">
            {{ record }}
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script>
// 🔵 引入你的测量脚本
import { measureData } from '@/assets/measure.js'

export default {
  data() {
    return {
      searchQuery: "",
      patients: [
        { id: 1, name: "患者1", history: [] },
        { id: 2, name: "患者2", history: [] },
        { id: 3, name: "患者3", history: [] }
      ],
      selectedPatient: null,
      show3DData: false,
      historyData: [],
      progress: 0 // 🔵 新增：测量进度
    };
  },
  computed: {
    filteredPatients() {
      if (!this.searchQuery) {
        return this.patients;
      }
      return this.patients.filter(patient =>
        patient.name.includes(this.searchQuery)
      );
    }
  },
  created() {
    this.patients.forEach(patient => {
      patient.history = this.generateRandomHistory();
    });
  },
  methods: {
    goBack() {
      this.$router.push({ name: "user" });
    },
    getPatientName(patientId) {
      const patient = this.patients.find(p => p.id === patientId);
      return patient ? patient.name : "";
    },
    showMeasurementData() {
      this.show3DData = true;
      this.historyData = [];
    },
    showHistoryData() {
      const patient = this.patients.find(p => p.id === this.selectedPatient);
      if (patient) {
        this.historyData = patient.history;
        this.show3DData = false;
      }
    },
    generateRandomHistory() {
      const records = [];
      const recordCount = Math.floor(Math.random() * 5) + 3;
      for (let i = 0; i < recordCount; i++) {
        const date = new Date(
          Date.now() - Math.random() * 10000000000
        ).toLocaleDateString();
        const value = (Math.random() * 100).toFixed(2);
        records.push(`日期: ${date} | 测量值: ${value}`);
      }
      return records;
    },
    // 🔵 修改后的测量方法
    measureDataAction() {
      this.progress = 0; // 开始测量时清空进度
      measureData({
        onProgress: (progress) => {
          this.progress = progress;
        },
        onComplete: (result) => {
          console.log('测量完成！结果：', result);

          // 找到当前选中的患者，添加测量结果
          const patient = this.patients.find(p => p.id === this.selectedPatient);
          if (patient) {
            const date = new Date().toLocaleDateString();
            const record = `日期: ${date} | 心率: ${result.heartRate} bpm | 血压: ${result.bloodPressure} | 体温: ${result.temperature} ℃`;
            patient.history.unshift(record); // 新数据加到最前面
          }

          // 测量完成后刷新历史记录展示
          this.showHistoryData();
          this.progress = 0; // 测量结束，清除进度显示
        }
      });
    }
  }
};
</script>

<style scoped>
.patient-manage {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 50px;
}
.background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('@/assets/fig1.png') no-repeat center center;
  background-size: cover;
  opacity: 0.5;
  z-index: 1;
}
.content {
  position: relative;
  z-index: 2;
  background: rgba(255, 255, 255, 0.85);
  padding: 30px;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
input {
  padding: 10px;
  margin: 10px;
  width: 200px;
}
select {
  padding: 10px;
  margin: 10px;
}
button {
  padding: 10px 20px;
  margin: 10px;
  cursor: pointer;
}
ul {
  list-style: none;
  padding: 0;
}
li {
  margin: 5px 0;
}
.progress-container {
  margin-top: 20px;
  text-align: center;
}
progress {
  width: 200px;
  height: 20px;
}
</style>
