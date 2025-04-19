// @/assets/measure.js

// 测量函数，接收回调，测量时不断更新状态
export function measureData({ onProgress, onComplete }) {
  console.log("开始测量数据...");

  let progress = 0;

  const interval = setInterval(() => {
    progress += Math.floor(Math.random() * 20) + 5; // 每次随机增加5~25%
    if (progress > 100) progress = 100;

    if (onProgress) {
      onProgress(progress); // 通知进度变化
    }

    if (progress === 100) {
      clearInterval(interval);

      const result = generateFakeResult();
      console.log("测量完成！结果:", result);

      if (onComplete) {
        onComplete(result); // 通知完成并给出结果
      }
    }
  }, 500); // 每0.5秒更新一次
}

// 生成一个假的测量结果
function generateFakeResult() {
  return {
    heartRate: Math.floor(Math.random() * 40) + 60,  // 60~100
    bloodPressure: `${Math.floor(Math.random() * 40) + 100}/${Math.floor(Math.random() * 20) + 70}`, // 100-140/70-90
    temperature: (36 + Math.random() * 2).toFixed(1) // 36.0~38.0℃
  };
}
