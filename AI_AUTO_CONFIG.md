# AI 自动生成视频模板配置

## 功能概述

系统支持上传视频模板后，由 AI（Claude Vision）自动分析视频内容，生成动态表单配置和视频叠加规则，无需人工创建 .doc 占位符模板。

## 工作流程

```
管理员上传视频模板
       ↓
AI 自动解析视频内容
（识别视频中需要用户填写的信息点位）
       ↓
生成动态表单配置
（姓名、年龄、照片上传等字段）
       ↓
管理员审核和修改 AI 生成的配置
       ↓
用户端观看视频 → 展示动态表单 → 用户填写
       ↓
AI 识别用户上传的素材并匹配到视频位置
       ↓
渲染生成个性化视频
```

## 使用步骤

### 1. 配置 Claude API Key

在 `application.properties` 中添加：

```properties
claude.api.key=sk-ant-api03-...
```

### 2. 创建模板

在管理后台创建新模板，填写基本信息（名称、价格、分类等）。

### 3. 上传视频

有两种方式上传视频并触发 AI 分析：

#### 方式 A：通过模板配置编辑器（推荐）

1. 点击模板的"配置"按钮
2. 在配置编辑器中上传视频文件
3. AI 自动分析，2分钟内完成

#### 方式 B：通过模板文件上传

1. 点击模板的"上传文件"
2. 选择 .mp4 视频文件
3. 系统自动触发 AI 分析

### 4. AI 分析过程

- **状态：analysing** - AI 正在分析视频
- **状态：done** - 分析完成，配置已生成
- **状态：failed** - 分析失败，可查看错误信息

AI 会：
- 从视频中均匀抽取 6 帧关键帧
- 识别视频中的动态内容位置（文字、图片占位符）
- 生成 `formFields`（用户填写的表单字段）
- 生成 `overlayRules`（视频叠加规则）

### 5. 审核和修改配置

AI 生成的配置会自动填入编辑器，管理员可以：

- **表单字段 (formFields)**
  - 修改字段 key、label、type
  - 调整必填项
  - 添加/删除字段

- **叠加规则 (overlayRules)**
  - 调整文字/图片的位置（x, y 坐标）
  - 修改显示时间（startTime, endTime）
  - 调整字体大小、颜色
  - 调整图片尺寸

### 6. 保存配置

点击"保存配置"按钮，配置生效。

## 配置格式说明

### formFields 格式

```json
[
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
  },
  {
    "key": "photo",
    "type": "image",
    "label": "照片",
    "placeholder": "请上传照片",
    "required": true
  }
]
```

### overlayRules 格式

```json
[
  {
    "key": "name",
    "type": "text",
    "startTime": 2.5,
    "endTime": 8.0,
    "x": 0.5,
    "y": 0.3,
    "fontSize": 60,
    "fontColor": "#ffffff"
  },
  {
    "key": "photo",
    "type": "image",
    "startTime": 5.0,
    "endTime": 15.0,
    "x": 0.3,
    "y": 0.4,
    "width": 0.3,
    "height": 0.4
  }
]
```

**坐标说明：**
- x, y: 0.0 到 1.0 的相对比例，原点在左上角
- width, height: 0.0 到 1.0 的相对比例
- startTime, endTime: 秒数

## API 接口

### 上传视频并触发分析

```http
POST /api/admin/templates/{templateId}/config/upload-video
Content-Type: multipart/form-data

file: <video file>
```

### 查询分析状态

```http
GET /api/admin/templates/{templateId}/config
```

响应：
```json
{
  "templateId": 1,
  "analysisStatus": "done",
  "analysisError": null,
  "formFields": [...],
  "overlayRules": [...]
}
```

### 保存配置

```http
PUT /api/admin/templates/{templateId}/config
Content-Type: application/json

{
  "formFields": "[...]",
  "overlayRules": "[...]"
}
```

## 核心代码

### 后端

- `TemplateAnalysisService.java` - AI 分析服务
- `AdminTemplateConfigController.java` - 配置管理接口
- `AdminTemplateController.java` - 模板上传接口

### 前端

- `TemplateConfigEditor.vue` - 配置编辑器组件
- `TemplatesView.vue` - 模板管理页面

## 注意事项

1. **Claude API Key 必须配置**，否则 AI 分析会失败
2. **视频格式**：支持 mp4, mov, avi
3. **分析时间**：通常 30-120 秒，取决于视频长度
4. **AI 准确性**：AI 生成的配置是草稿，建议人工审核调整
5. **失败处理**：如果 AI 分析失败，可以手动配置表单和叠加规则

## 优势

✅ **无需手工创建 .doc 模板** - AI 自动识别视频内容  
✅ **快速上架** - 2分钟内完成配置  
✅ **灵活调整** - 管理员可随时修改 AI 生成的配置  
✅ **降低门槛** - 不需要了解 Word 占位符语法  
✅ **提高效率** - 批量上架视频模板更快捷
