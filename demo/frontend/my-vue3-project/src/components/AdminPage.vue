<template>
  <div class="admin">
    <div class="background"></div> <!-- 背景层 -->

    <div class="admin-content">
      <h2>管理员界面 - 医生与患者关系管理</h2>

      <!-- 注册医生表单 --> <!-- 新增 -->
      <div class="form">
        <input v-model="newDoctorUsername" type="text" placeholder="医生用户名" />
        <input v-model="newDoctorPassword" type="password" placeholder="医生密码" />
        <button @click="registerDoctor">注册医生</button>
      </div>

      <!-- 新增关系表单 -->
      <div class="form">
        <input v-model="newDoctor" type="text" placeholder="医生姓名" />
        <input v-model="newPatient" type="text" placeholder="患者姓名" />
        <button @click="addRelation">添加关系</button>
      </div>

      <!-- 关系列表 -->
      <table>
        <thead>
          <tr>
            <th>医生</th>
            <th>患者</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(relation, index) in relations" :key="index">
            <td>
              <input v-model="relation.doctor" />
            </td>
            <td>
              <input v-model="relation.patient" />
            </td>
            <td>
              <button @click="updateRelation(index)">保存</button>
              <button @click="deleteRelation(index)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 医生账户列表展示 --> <!-- 新增 -->
      <h3 style="margin-top: 30px;">已注册医生账户</h3>
      <table>
        <thead>
          <tr>
            <th>用户名</th>
            <th>密码</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(doctor, index) in doctors" :key="index">
            <td>{{ doctor.username }}</td>
            <td>{{ doctor.password }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>


<script>
export default {
  name: "AdminPage",
  data() {
    return {
      relations: [],
      doctors: [],
      newDoctor: "",
      newPatient: "",
      newDoctorUsername: "",
      newDoctorPassword: "",
    };
  },
  methods: {
    // 添加医生与患者关系
    addRelation() {
      if (!this.newDoctor || !this.newPatient) {
        alert("医生和患者姓名都不能为空！");
        return;
      }
      this.relations.push({
        doctor: this.newDoctor,
        patient: this.newPatient,
      });
      this.newDoctor = "";
      this.newPatient = "";
      alert("关系添加成功！");
    },
    // 注册医生账户（用户名+密码）
    registerDoctor() {
      if (!this.newDoctorUsername || !this.newDoctorPassword) {
        alert("用户名和密码都不能为空！");
        return;
      }
      const exists = this.doctors.some(doc => doc.username === this.newDoctorUsername);
      if (exists) {
        alert("用户名已存在！");
        return;
      }
      this.doctors.push({
        username: this.newDoctorUsername,
        password: this.newDoctorPassword,
      });
      this.newDoctorUsername = "";
      this.newDoctorPassword = "";
      alert("医生注册成功！");
    },
    // 删除医生与患者关系
    deleteRelation(index) {
      if (confirm("确定要删除这条关系吗？")) {
        this.relations.splice(index, 1);
        alert("删除成功！");
      }
    },
    // 更新医生与患者关系
    updateRelation(index) {
      const relation = this.relations[index];
      if (!relation.doctor || !relation.patient) {
        alert("医生和患者姓名都不能为空！");
        return;
      }
      alert("修改成功！");
    },
    // 解析 医生-患者关系 CSV
    parseDoctorPatientCSV(csv) {
      const lines = csv.trim().split('\n');
      return lines.map(line => {
        const [doctor, patient] = line.split(',');
        return { doctor: doctor.trim(), patient: patient.trim() };
      });
    },
    // 解析 医生账户 CSV
    parseDoctorAccountCSV(csv) {
      const lines = csv.trim().split('\n');
      return lines.map(line => {
        const [username, password] = line.split(',');
        return { username: username.trim(), password: password.trim() };
      });
    },
    // 加载 CSV 文件
    async loadCSVData() {
      try {
        // 加载 医生与患者关系
        const relationResponse = await fetch('/DoctorPatientRelationship.csv');
        const relationText = await relationResponse.text();
        this.relations = this.parseDoctorPatientCSV(relationText);

        // 加载 医生账户
        const doctorResponse = await fetch('/DoctorAccount.csv');
        const doctorText = await doctorResponse.text();
        this.doctors = this.parseDoctorAccountCSV(doctorText);
      } catch (error) {
        console.error("加载CSV文件失败：", error);
        alert("加载初始数据失败，请检查文件路径或服务器设置！");
      }
    },
  },
  mounted() {
    this.loadCSVData();
  }
};
</script>


<style scoped>
.admin {
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
.admin-content {
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

.form {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 20px;
}

input {
  padding: 8px;
  width: 150px;
}

button {
  padding: 8px 12px;
  cursor: pointer;
  margin: 2px;
  border: none;
  background-color: #4caf50;
  color: white;
  border-radius: 5px;
  transition: background-color 0.3s;
}

button:hover {
  background-color: #45a049;
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
}
</style>
