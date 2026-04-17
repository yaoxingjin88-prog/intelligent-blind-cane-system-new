# 编码规范

## Java编码规范（后端）

### 命名规范

#### 类名
- 使用大驼峰命名法
- 名词或名词短语
- 示例：`UserService`, `DeviceController`

#### 方法名
- 使用小驼峰命名法
- 动词或动词短语
- 示例：`getUserById()`, `createDevice()`

#### 变量名
- 使用小驼峰命名法
- 见名知意
- 示例：`userName`, `deviceList`

#### 常量名
- 全大写，下划线分隔
- 示例：`MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE`

#### 包名
- 全小写，点分隔
- 反域名规则
- 示例：`com.ruoyi.service`

### 代码结构

#### Controller层
- 负责请求处理
- 参数校验
- 调用Service层
- 返回统一响应

```java
@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    
    @Autowired
    private DeviceService deviceService;
    
    @GetMapping("/{id}")
    public Result<DeviceVO> getDevice(@PathVariable Long id) {
        DeviceVO device = deviceService.getDeviceById(id);
        return Result.success(device);
    }
}
```

#### Service层
- 业务逻辑处理
- 事务控制
- 调用Mapper层

```java
@Service
@Transactional
public class DeviceServiceImpl implements DeviceService {
    
    @Autowired
    private DeviceMapper deviceMapper;
    
    @Override
    public DeviceVO getDeviceById(Long id) {
        Device device = deviceMapper.selectById(id);
        return BeanUtil.copyProperties(device, DeviceVO.class);
    }
}
```

#### Mapper层
- 数据访问
- SQL映射
- 使用MyBatis注解或XML

```java
@Mapper
public interface DeviceMapper {
    
    @Select("SELECT * FROM cane_device WHERE id = #{id}")
    Device selectById(Long id);
}
```

### 注释规范

#### 类注释
```java
/**
 * 设备服务实现类
 * 
 * @author 作者名
 * @since 2024-01-01
 */
```

#### 方法注释
```java
/**
 * 根据ID获取设备信息
 * 
 * @param id 设备ID
 * @return 设备信息
 * @throws BusinessException 设备不存在时抛出
 */
```

### 异常处理

#### 自定义异常
```java
public class BusinessException extends RuntimeException {
    private String code;
    private String message;
}
```

#### 全局异常处理
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }
}
```

## JavaScript/TypeScript编码规范（前端）

### 命名规范

#### 组件名
- 使用大驼峰命名法
- 示例：`UserCard.vue`, `DeviceList.vue`

#### 函数/方法名
- 使用小驼峰命名法
- 动词开头
- 示例：`getUserInfo()`, `handleClick()`

#### 变量名
- 使用小驼峰命名法
- 示例：`userName`, `deviceList`

#### 常量名
- 全大写，下划线分隔
- 示例：`API_BASE_URL`, `MAX_RETRY_COUNT`

### Vue组件规范

#### 组件结构
```vue
<template>
  <!-- 模板 -->
</template>

<script setup lang="ts">
// 逻辑
</script>

<style lang="scss" scoped>
// 样式
</style>
```

#### 命名顺序
1. imports
2. props
3. emits
4. refs
5. computed
6. methods
7. lifecycle hooks

### TypeScript规范

#### 类型定义
```typescript
interface User {
  id: number;
  name: string;
  email: string;
}
```

#### 类型注解
```typescript
function getUserById(id: number): Promise<User> {
  // 实现
}
```

## 小程序编码规范

### 页面命名
- 使用kebab-case
- 示例：`user-profile.vue`, `device-list.vue`

### 组件命名
- 使用PascalCase
- 示例：`UserCard.vue`, `DeviceStatus.vue`

### API调用
```typescript
import { getDeviceList } from '@/api/device'

const devices = await getDeviceList()
```

## 通用规范

### 代码格式化
- 使用统一的代码格式化工具
- 前端使用Prettier
- 后端使用IDE格式化

### Git提交
```
feat: 添加设备管理功能
fix: 修复登录bug
docs: 更新README
style: 格式化代码
refactor: 重构用户模块
perf: 优化查询性能
test: 添加单元测试
chore: 更新依赖版本
```

### 代码审查要点
- 代码风格是否统一
- 是否有明显的bug
- 是否有安全漏洞
- 是否有性能问题
- 是否有冗余代码
- 注释是否充分

## 安全规范

### SQL注入防护
- 使用参数化查询
- 使用MyBatis参数绑定

### XSS防护
- 前端输入过滤
- 后端输出转义

### CSRF防护
- 使用CSRF Token
- 验证Referer

### 敏感信息保护
- 不在代码中硬编码密码
- 使用环境变量
- 加密存储敏感数据
