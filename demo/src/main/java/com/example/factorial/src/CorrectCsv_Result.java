package com.example.factorial.src;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CorrectCsv_Result {
    public static void Save(){
        List<String[]> csvData = generateCsvData();
        saveToCsv(csvData, "sentiment_data.csv");
    }
    public static List<String[]> generateCsvData() {
        List<String[]> data = new ArrayList<>();

        // 表头（保持原格式）
        data.add(new String[]{"date", "type", "summary", "运动幅度", "得分"});

        Random random = new Random();
        LocalDate currentDate = LocalDate.now();

        for (int i = 0; i < 5; i++) {
            LocalDate date = currentDate.minusDays(i * 2);

            // 生成运动幅度（12个值）
            int[] motionData = new int[12];
            for (int j = 0; j < 12; j++) {
                motionData[j] = 10 + random.nextInt(15); // 10-25的随机数
            }

            // 生成得分（12个值）
            int[] scoreData = new int[12];
            for (int j = 0; j < 12; j++) {
                scoreData[j] = 40 + random.nextInt(60); // 40-100的随机数
            }

            // 添加到数据行（数组转为字符串格式）
            data.add(new String[]{
                    date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    "步态分析",
                    "最新步态分析报告",
                    arrayToString(motionData),
                    arrayToString(scoreData)
            });
        }
        return data;
    }

    // 将数组转为 "[x,y,z,...]" 格式的字符串
    private static String arrayToString(int[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append("，");//!!!!!中文逗号   千万别改
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }
    public static void saveToCsv(List<String[]> data, String filename) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8))) {
            writer.write("\uFEFF");
            // 写入数据
            for (String[] row : data) {
                writer.write(String.join(",", row));
                writer.newLine();
            }
            System.out.println("CSV文件已保存: " + filename);
        } catch (IOException e) {
            System.err.println("保存失败: " + e.getMessage());
        }
    }
}
