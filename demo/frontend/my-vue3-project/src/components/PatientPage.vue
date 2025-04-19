<template>
  <div class="patient">
    <div class="background"></div> <!-- 背景层 -->

    <div class="patient-content">
      <h2>患者界面 - 历史健康数据</h2>

      <!-- 搜索功能 -->
      <div class="search">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索记录 (日期/体温/血压)"
        />
      </div>

      <div class="record-list">
        <table>
          <thead>
            <tr>
              <th @mouseover="sortTable('date')">日期</th>
              <th @mouseover="sortTable('temperature')">体温 (℃)</th>
              <th @mouseover="sortTable('bloodPressure')">血压 (mmHg)</th>
              <th>就诊记录</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(record, index) in filteredRecords" :key="index">
              <td>{{ record.date }}</td>
              <td>{{ record.temperature }}</td>
              <td>{{ record.bloodPressure }}</td>
              <td>{{ record.visit }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "PatientHistoryPage",
  data() {
    return {
      searchQuery: "",
      records: [
        {
          date: "2025-04-10",
          temperature: "36.5",
          bloodPressure: "120/80",
          visit: "常规体检",
        },
        {
          date: "2025-03-15",
          temperature: "37.2",
          bloodPressure: "130/85",
          visit: "感冒复诊",
        },
        {
          date: "2025-02-05",
          temperature: "36.8",
          bloodPressure: "118/78",
          visit: "年度体检",
        },
      ],
    };
  },
  computed: {
    // 筛选记录
    filteredRecords() {
      return this.records.filter((record) => {
        return (
          record.date.includes(this.searchQuery) ||
          record.temperature.includes(this.searchQuery) ||
          record.bloodPressure.includes(this.searchQuery) ||
          record.visit.includes(this.searchQuery)
        );
      });
    },
  },
  methods: {
    // 排序功能（按日期、体温或血压排序）
    sortTable(property) {
      this.records.sort((a, b) => {
        if (a[property] > b[property]) return 1;
        if (a[property] < b[property]) return -1;
        return 0;
      });
    },
  },
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
  background: rgba(255, 255, 255, 0.8);
  padding: 40px;
  border-radius: 10px;
  width: 800px;
  max-height: 90vh;
  overflow-y: auto;
}

h2 {
  text-align: center;
  margin-bottom: 20px;
}

.search {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
}

input {
  padding: 8px;
  width: 200px;
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
</style>