<template> 
  <div class="patient">
    <div class="background"></div> <!-- 背景层 -->

    <div class="patient-content">
      <h2>患者界面 - 步态分析报告</h2>

      <!-- 报告选择 -->
      <div class="report-selection">
        <label for="report-select">选择报告：</label>
        <select id="report-select" v-model="selectedReport" @change="loadReportData">
          <option v-for="(report, index) in reports" :key="index" :value="index">
            {{ report.date }} - {{ report.type }}
          </option>
        </select>
      </div>

      <!-- 步态分析数据展示 -->
      <div class="gait-analysis">
        <h3>步态分析数据</h3>
        <table>
          <thead>
            <tr>
              <th>指标</th>
              <th v-for="(col, index) in gaitData.columns" :key="index">{{ col }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, rowIndex) in gaitData.rows" :key="rowIndex">
              <td>{{ row.name }}</td>
              <td v-for="(value, colIndex) in row.values" :key="colIndex">{{ value }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 搜索功能 -->
      <div class="search">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索历史记录 (日期/类型)"
        />
      </div>

      <div class="record-list">
        <table>
          <thead>
            <tr>
              <th @click="sortTable('date')">日期</th>
              <th @click="sortTable('type')">报告类型</th>
              <th @click="sortTable('summary')">摘要</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(record, index) in filteredRecords" :key="index" @click="selectReport(index)">
              <td>{{ record.date }}</td>
              <td>{{ record.type }}</td>
              <td>{{ record.summary }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
<script>
import Papa from 'papaparse'; // 用PapaParse解析CSV

export default {
  name: "PatientHistoryPage",
  data() {
    return {
      searchQuery: "",
      selectedReport: 0,
      gaitData: {
        columns: ['L1', 'L2', 'L3', 'L4', 'L5', 'L6', 'R1', 'R2', 'R3', 'R4', 'R5', 'R6'],
        rows: [
          { name: '运动幅度', values: [] },
          { name: '得分', values: [] }
        ]
      },
      reports: [],
      sortField: '',
      sortDirection: 1
    };
  },
  computed: {
    filteredRecords() {
      return this.reports.filter((record) => {
        return (
          record.date.includes(this.searchQuery) ||
          record.type.includes(this.searchQuery) ||
          record.summary.includes(this.searchQuery)
        );
      });
    },
  },
  methods: {
    loadReportData() {
      const report = this.reports[this.selectedReport];
      this.gaitData.rows[0].values = report.data['运动幅度'];
      this.gaitData.rows[1].values = report.data['得分'];
    },
    selectReport(index) {
      this.selectedReport = index;
      this.loadReportData();
    },
    sortTable(property) {
      if (this.sortField === property) {
        this.sortDirection *= -1;
      } else {
        this.sortField = property;
        this.sortDirection = 1;
      }

      this.reports.sort((a, b) => {
        if (a[property] > b[property]) return 1 * this.sortDirection;
        if (a[property] < b[property]) return -1 * this.sortDirection;
        return 0;
      });
    },
    fetchCSVData() {
      Papa.parse('/sentiment_data.csv', {
        download: true,
        header: true,
        complete: (result) => {
          const csvData = result.data;

          this.reports = csvData.map(item => ({
            date: item.date,
            type: item.type,
            summary: item.summary,
            data: {
              '运动幅度': JSON.parse(item.运动幅度 || '[]'),
              '得分': JSON.parse(item.得分 || '[]')
            }
          }));

          if (this.reports.length > 0) {
            this.selectedReport = 0;
            this.loadReportData();
          }
        },
        error: (error) => {
          console.error('读取CSV文件出错:', error);
        }
      });
    }
  },
  mounted() {
    this.fetchCSVData();
  }
};
</script>


<style scoped>
.patient {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
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
.patient-content {
  position: relative;
  z-index: 2;
  background: rgba(255, 255, 255, 0.9);
  padding: 40px;
  border-radius: 10px;
  width: 900px;
  max-height: 90vh;
  overflow-y: auto;
}

h2, h3 {
  text-align: center;
  margin-bottom: 20px;
}

.report-selection {
  margin-bottom: 20px;
  text-align: center;
}

.report-selection select {
  padding: 8px;
  width: 300px;
  border-radius: 5px;
  border: 1px solid #ccc;
}

.gait-analysis {
  margin-bottom: 30px;
  background: #f9f9f9;
  padding: 15px;
  border-radius: 5px;
}

.gait-analysis table {
  width: 100%;
  margin-top: 10px;
}

.search {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
}

input {
  padding: 8px;
  width: 250px;
  border-radius: 5px;
  border: 1px solid #ccc;
}

.record-list {
  width: 100%;
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
}

th, td {
  border: 1px solid #ccc;
  padding: 10px;
  text-align: center;
}

th {
  background-color: #f0f0f0;
  cursor: pointer;
}

th:hover {
  background-color: #ddd;
}

.record-list tbody tr:hover {
  background-color: #f5f5f5;
  cursor: pointer;
}
</style>