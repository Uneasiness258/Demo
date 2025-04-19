<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <div class="login">
    <div class="background"></div>
    <div class="login-content">
      <h2 v-if="!isRegistering">登录</h2>
      <h2 v-else>注册</h2>

      <input v-model="username" type="text" placeholder="请输入用户名" />
      <input v-model="password" type="password" placeholder="请输入密码" />

      <!-- 修正：将 login 改为 handleLogin -->
      <button v-if="!isRegistering" @click="handleLogin">登录</button>
      <button v-else @click="register">注册</button>

      <p class="toggle" @click="toggleRegister">
        {{ isRegistering ? '已有账号？点这里登录' : '没有账号？点这里注册' }}
      </p>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      username: "",
      password: "",
      loading: false,
      isRegistering: false
    };
  },
  methods: {
    async handleLogin() {
      // 1. 前端验证
      if (!this.username || !this.password) {
        alert('用户名和密码不能为空'); // 直接弹窗提示
        return;
      }

      this.loading = true;

      try {
        // 2. 调用后端API
        const response = await axios.post('/api/login', {
          username: this.username,
          password: this.password
        }, {
          headers: {
            'Content-Type': 'application/json'
          }
        });

        // 3. 登录成功处理
        if (response.status === 200) {
          // 根据用户名跳转
          if (this.username === 'Admin') {
                  // 管理员跳转
                  this.$router.push({ name: 'AdminPage' });
                } else if (/^DOC\d+$/.test(this.username)) {
                  // 医生跳转：DOC开头+数字
                  this.$router.push({ name: 'PatientManage' });
                } else if (/^\d+$/.test(this.username)) {
                  // 患者跳转：纯数字
                  this.$router.push({ name: 'PatientPage' });
                }
        }

      } catch (error) {
        // 4. 错误处理（直接弹窗）
        if (error.response && error.response.status === 401) {
          alert('用户名或密码错误'); // 401错误提示
        } else {
          alert('登录失败，请检查网络连接'); // 其他错误提示
        }
        console.error('登录错误:', error);
      } finally {
        this.loading = false;
      }
    },

    // 其他方法保持不变...
    async register() {
      // 1. 前端验证
      if (!this.username || !this.password) {
        alert("请填写用户名和密码！");
        return;
      }

      this.loading = true; // 显示加载状态

      try {
        // 2. 调用后端API
        const response = await axios.post('/api/Signup', {
          username: this.username,
          password: this.password
        }, {
          headers: {
            'Content-Type': 'application/json'
          }
        });

        // 3. 处理注册成功
        if (response.status === 200) {
          alert(`注册成功！欢迎你，${this.username}！`);
        }

      } catch (error) {
        // 4. 错误处理
        if (error.response) {
          // 有响应但状态码不是200
          if (error.response.status === 401) {
            alert('用户名已存在或格式不正确');
          } else {
            alert(`注册失败: ${error.response.data?.message || '服务器错误'}`);
          }
        } else if (error.request) {
          // 请求已发出但没有响应
          alert('网络错误，请检查连接');
        }
        console.error('注册错误:', error);

      } finally {
        this.loading = false; // 隐藏加载状态
        this.resetForm(); // 无论成功失败都重置表单
      }
    },

    toggleRegister() {
      this.isRegistering = !this.isRegistering;
      this.resetForm();
    },

    resetForm() {
      this.username = "";
      this.password = "";
    }
  }
};
</script>

<style scoped>
.login {
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
.login-content {
  position: relative;
  z-index: 2;
  background: rgba(255, 255, 255, 0.8);
  padding: 40px;
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
button {
  padding: 10px 20px;
  cursor: pointer;
  margin-top: 10px;
}
.toggle {
  margin-top: 15px;
  color: #007bff;
  cursor: pointer;
  font-size: 14px;
  text-decoration: underline;
}
.toggle:hover {
  color: #0056b3;
}
</style>
