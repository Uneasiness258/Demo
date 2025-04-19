<template>
  <div class="admin">
    <div class="background"></div> <!-- 背景层 -->

    <div class="admin-content">
      <h2>管理员界面 - 医生与患者关系管理</h2>

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
    </div>
  </div>
</template>

<script>
export default {
  name: "AdminPage",
  data() {
    return {
      relations: [
        { doctor: "张医生", patient: "李患者" },
        { doctor: "王医生", patient: "赵患者" },
      ],
      newDoctor: "",
      newPatient: "",
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
      // 模拟保存数据操作
      alert("修改成功！");
    },
  },
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
