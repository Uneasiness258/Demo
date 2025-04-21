<template>
  <div class="patient-manage">
    <div class="background"></div>

    <div class="content">
      <h2>患者管理</h2>

      <button @click="goBack">返回用户界面</button>

      <div>
        <input v-model="searchQuery" type="text" placeholder="搜索患者姓名..." />
      </div>

      <div v-for="patient in filteredPatients" :key="patient.id" class="patient-card">
        <div class="patient-header">
          <h3>{{ patient.name }}</h3>
          <button @click="toggleDetails(patient.id)">
            {{ expandedPatients.includes(patient.id) ? '收起详情' : '查看详情' }}
          </button>
          <button @click="measureDataAction(patient.id)">测量数据</button>
        </div>

        <div v-if="expandedPatients.includes(patient.id)" class="patient-details">
          <button @click="showMeasurementData(patient.id)">查看测量数据</button>
          <button @click="showHistoryData(patient.id)">查看历史数据</button>

          <div v-if="currentPatientId === patient.id">
            <div v-if="progress > 0 && progress < 100" class="progress-container">
              <progress :value="progress" max="100"></progress>
              <div>{{ progress }}%</div>
            </div>

            <div v-if="show3DData">
              <h4>3D测量数据展示</h4>
              <img src="@/assets/3d-measurement.gif" alt="3D Measurement Data" />
            </div>

            <div v-if="historyData.length">
              <h4>历史数据</h4>
              <ul>
                <li v-for="(record, index) in historyData" :key="index" style="margin-bottom: 10px;">
                  {{ record.date }} | 类型: {{ record.type }} | 摘要: {{ record.summary }}
                  <button @click="toggleReport(index)">
                    {{ expandedReports.includes(index) ? '收起' : '展开' }}
                  </button>

                  <div v-if="expandedReports.includes(index)" class="detail-section">
                    <table class="gait-table">
                      <thead>
                        <tr>
                          <th>指标</th>
                          <th v-for="(col, idx) in gaitColumns" :key="'header-' + idx">{{ col }}</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td>运动幅度</td>
                          <td v-for="(value, idx) in record.data['运动幅度']" :key="'move-' + idx">{{ value }}</td>
                        </tr>
                        <tr>
                          <td>得分</td>
                          <td v-for="(value, idx) in record.data['得分']" :key="'score-' + idx">{{ value }}</td>
                        </tr>
                      </tbody>
                    </table>

                  </div>

                </li>
              </ul>
            </div>

          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Papa from 'papaparse'; // 引入PapaParse解析CSV

export default {
  data() {
    return {
      searchQuery: "",
      patients: [
        { id: 1, name: "患者1" },
        { id: 2, name: "患者2" },
        { id: 3, name: "患者3" }
      ],
      expandedPatients: [],
      show3DData: false,
      historyData: [],
      currentPatientId: null,
      progress: 0,
      allReports: [],        // 所有CSV数据
      expandedReports: [],   // 当前展开的历史数据索引
      gaitColumns: ['L1', 'L2', 'L3', 'L4', 'L5', 'L6', 'R1', 'R2', 'R3', 'R4', 'R5', 'R6']
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
    this.fetchCSVData();

    if (this.$route.query.measureResult && this.$route.query.patientId) {
      const result = JSON.parse(this.$route.query.measureResult);
      const patientId = parseInt(this.$route.query.patientId);

      const patient = this.patients.find(p => p.id === patientId);
      if (patient) {
        const date = new Date().toLocaleDateString();
        const record = `日期: ${date} | 心率: ${result.heartRate} bpm | 血压: ${result.bloodPressure} | 体温: ${result.temperature} ℃`;
        if (!patient.history) patient.history = [];
        patient.history.unshift(record);
      }
    }
  },
  methods: {
    goBack() {
      this.$router.push({ name: "user" });
    },
    toggleDetails(patientId) {
      const index = this.expandedPatients.indexOf(patientId);
      if (index > -1) {
        this.expandedPatients.splice(index, 1);
      } else {
        this.expandedPatients.push(patientId);
      }
      this.show3DData = false;
      this.historyData = [];
      this.currentPatientId = null;
    },
    showMeasurementData(patientId) {
      this.currentPatientId = patientId;
      this.show3DData = true;
      this.historyData = [];
    },
    showHistoryData(patientId) {
      this.currentPatientId = patientId;
      this.show3DData = false;
      this.historyData = this.allReports
        .filter(report => report.date && report.type && report.summary)
        .map(report => ({
          date: report.date,
          type: report.type,
          summary: report.summary,
          data: report.data
        }));
      this.expandedReports = [];
    },
    toggleReport(index) {
      const pos = this.expandedReports.indexOf(index);
      if (pos > -1) {
        this.expandedReports.splice(pos, 1);
      } else {
        this.expandedReports.push(index);
      }
    },
    measureDataAction(patientId) {
      this.$router.push({
        name: "BlueTooth",
        query: { patientId: patientId }
      });
    },
    fetchCSVData() {
      Papa.parse('/sentiment_data.csv', {
        download: true,
        header: true,
        complete: (result) => {
          const csvData = result.data;
          this.allReports = csvData.map(item => ({
            date: item.date,
            type: item.type,
            summary: item.summary,
            data: {
              '运动幅度': JSON.parse(item.运动幅度 || '[]'),
              '得分': JSON.parse(item.得分 || '[]')
            }
          }));
        },
        error: (error) => {
          console.error('读取CSV出错:', error);
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
.detail-section {
  margin-top: 10px;
  padding: 10px;
  background: #f0f9ff;
  border-radius: 6px;
}
.gait-table {
  width: 100%;
  margin-top: 10px;
  border-collapse: collapse;
}
.gait-table th,
.gait-table td {
  border: 1px solid #ccc;
  padding: 8px;
  text-align: center;
}
.gait-table th {
  background-color: #f7f7f7;
}

</style>
