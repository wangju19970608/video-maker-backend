# 快速测试指南

## 测试 AI 自动生成配置功能

### 前置条件

1. 确保 `application.properties` 中配置了 Claude API Key：
   ```properties
   claude.api.key=sk-ant-api03-...
   ```

2. 确保 FFmpeg 已安装并可用（用于视频帧提取）

3. 启动后端服务和前端管理后台

### 测试步骤

#### 1. 创建测试模板

访问管理后台 → 模板管理 → 新建模板

填写基本信息：
- 模板编码：`test-ai-001`
- 模板名称：`AI测试模板`
- 主题：`birthday`
- 分类：`kids`
- 价格：`19.9`

保存后记录模板 ID（例如：123）

#### 2. 上传视频触发 AI 分析

**方式 A：通过配置编辑器**

1. 点击模板的"配置"按钮
2. 在"叠加规则"标签页，点击"上传模板视频"
3. 选择一个测试视频（建议：生日祝福视频，包含文字和照片占位）
4. 观察页面显示"🤖 AI分析中..."
5. 等待 30-120 秒

**方式 B：通过 API 测试**

```bash
curl -X POST http://localhost:8080/api/admin/templates/123/config/upload-video \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@test-video.mp4"
```

#### 3. 查看分析结果

**前端查看：**
- 分析完成后，页面会显示"AI分析完成，规则已自动填入"
- 表单字段和叠加规则会自动填充到编辑器中

**API 查看：**
```bash
curl http://localhost:8080/api/admin/templates/123/config \
  -H "Authorization: Bearer YOUR_TOKEN"
```

响应示例：
```json
{
  "code": 0,
  "data": {
    "templateId": 123,
    "analysisStatus": "done",
    "analysisError": null,
    "formFields": [
      {
        "key": "name",
        "type": "text",
        "label": "姓名",
        "placeholder": "请输入姓名",
        "required": true
      },
      {
        "key": "age",
        "type": "number",
        "label": "年龄",
        "placeholder": "请输入年龄",
        "required": true
      }
    ],
    "overlayRules": [
      {
        "key": "name",
        "type": "text",
        "startTime": 2.5,
        "endTime": 8.0,
        "x": 0.5,
        "y": 0.3,
        "fontSize": 60,
        "fontColor": "#ffffff"
      }
    ]
  }
}
```

#### 4. 修改和保存配置

1. 在编辑器中调整 AI 生成的配置：
   - 修改字段标签
   - 调整坐标位置
   - 修改字体大小/颜色
   - 添加/删除字段

2. 点击"保存配置"

3. 验证保存成功

#### 5. 测试用户端流程

1. 访问用户端，选择该模板
2. 查看是否显示动态表单（AI 生成的字段）
3. 填写表单并提交
4. 生成视频，验证叠加效果

### 常见问题排查

#### AI 分析失败

**检查日志：**
```bash
tail -f birthday/video-maker-backend/backend.log
```

**常见原因：**
1. Claude API Key 未配置或无效
2. FFmpeg 未安装或路径错误
3. 视频文件损坏或格式不支持
4. 网络连接问题（无法访问 Claude API）

**解决方法：**
- 检查 `application.properties` 中的 `claude.api.key`
- 运行 `ffmpeg -version` 确认 FFmpeg 可用
- 尝试使用标准 mp4 格式的视频
- 检查网络代理设置

#### AI 生成的配置不准确

**原因：**
- 视频内容复杂，AI 识别困难
- 视频中没有明显的占位符标记

**解决方法：**
- 手动调整 AI 生成的配置
- 在视频中使用明显的占位符（如：{{姓名}}、{{照片}}）
- 使用对比度高的文字和背景

#### 分析超时

**原因：**
- 视频过长（超过 5 分钟）
- Claude API 响应慢

**解决方法：**
- 使用较短的视频（建议 30 秒 - 2 分钟）
- 增加超时时间（修改 `TemplateAnalysisService.java` 中的 `setReadTimeout`）

### 性能测试

**测试场景：**
- 视频长度：30 秒
- 视频分辨率：1920x1080
- 关键帧数量：6 帧

**预期结果：**
- 帧提取时间：< 10 秒
- Claude API 调用时间：30-60 秒
- 总分析时间：< 90 秒

### 日志示例

**成功日志：**
```
[INFO] Starting analysis for template 123
[INFO] Extracted 6 frames from video
[INFO] Calling Claude Vision API...
[INFO] Claude API response received
[INFO] Analysis completed successfully
[INFO] Generated 3 form fields and 4 overlay rules
```

**失败日志：**
```
[ERROR] Analysis failed for template 123
[ERROR] Claude API returned error 401: Invalid API key
[ERROR] Analysis status set to 'failed'
```

### 测试清单

- [ ] 创建测试模板
- [ ] 上传视频文件
- [ ] AI 分析成功完成
- [ ] 生成的 formFields 合理
- [ ] 生成的 overlayRules 合理
- [ ] 手动修改配置
- [ ] 保存配置成功
- [ ] 用户端显示动态表单
- [ ] 视频生成叠加效果正确
- [ ] 错误处理正常（API Key 错误、视频格式错误等）

### 下一步

测试通过后，可以：
1. 批量上传视频模板
2. 优化 AI Prompt 提高识别准确率
3. 添加更多字段类型支持（日期、下拉框等）
4. 实现视频预览功能（在编辑器中预览叠加效果）
