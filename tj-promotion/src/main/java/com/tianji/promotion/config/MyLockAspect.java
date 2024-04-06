package com.tianji.promotion.config;

import com.tianji.common.utils.StringUtils;
import com.tianji.promotion.annotation.MyLock;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * MyLock切面类
 */
@Component
@Aspect
@RequiredArgsConstructor
public class MyLockAspect implements Ordered {

    private final RedissonClient redissonClient;
    private final MyLockFactory myLockFactory;

    // 注解方式注明切入点，myLock要和方法列表中的myLock名称保持一致
    @Around("@annotation(myLock)")
    public Object tryLock(ProceedingJoinPoint pjp, MyLock myLock) throws Throwable {
        // 0. SPEL表达式拼接动态锁名称
        String lockName = getLockName(myLock.name(), pjp);
        // 1.创建锁对象(更新为工厂模式创建)
        // RLock lock = redissonClient.getLock(myLock.name());  // 只能获取可重入锁
        RLock lock = myLockFactory.getLock(myLock.lockType(), lockName);

        // 2.尝试获取锁
        // boolean isLock = lock.tryLock(myLock.waitTime(), myLock.leaseTime(), myLock.unit()); // 无重试，指定重试
        boolean isLock = myLock.lockStrategy().tryLock(lock, myLock);   // 采用策略模式，让用户选择想要的策略
        // 3.判断是否成功
        if (!isLock) {
            // 3.1.失败，快速结束
            // throw new BizIllegalException("请求太频繁，请稍后重试");
            return null; // 这部分代码不再需要，失败处理已在lockStragety定义
        }
        try {
            // 3.2.成功，执行业务
            return pjp.proceed();
        } finally {
            // 4.释放锁
            lock.unlock();
        }
    }

    @Override
    public int getOrder() {
        // order越小，优先级越高
        // EnableTransactionManagement为Integer.MAV_VALUE
        return 0;
    }


    /**
     * SPEL的正则规则
     */
    private static final Pattern pattern = Pattern.compile("\\#\\{([^\\}]*)\\}");
    /**
     * 方法参数解析器
     */
    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 解析锁名称
     * @param name 原始锁名称
     * @param pjp 切入点
     * @return 解析后的锁名称
     */
    private String getLockName(String name, ProceedingJoinPoint pjp) {
        // 1.判断是否存在spel表达式
        if (StringUtils.isBlank(name) || !name.contains("#")) {
            // 不存在，直接返回
            return name;
        }
        // 2.构建context,也就是SPEL表达式获取参数的上下文环境，这里上下文就是切入点的参数列表
        EvaluationContext context = new MethodBasedEvaluationContext(
                TypedValue.NULL, resolveMethod(pjp), pjp.getArgs(), parameterNameDiscoverer);
        // 3.构建SPEL解析器
        ExpressionParser parser = new SpelExpressionParser();
        // 4.循环处理，因为表达式中可以包含多个表达式
        Matcher matcher = pattern.matcher(name);
        while (matcher.find()) {
            // 4.1.获取表达式
            String tmp = matcher.group();
            String group = matcher.group(1);
            // 4.2.这里要判断表达式是否以 T字符开头，这种属于解析静态方法，不走上下文
            Expression expression = parser.parseExpression(group.charAt(0) == 'T' ? group : "#" + group);
            // 4.3.解析出表达式对应的值
            Object value = expression.getValue(context);
            // 4.4.用值替换锁名称中的SPEL表达式
            name = name.replace(tmp, ObjectUtils.nullSafeToString(value));
        }
        return name;
    }

    private Method resolveMethod(ProceedingJoinPoint pjp) {
        // 1.获取方法签名
        MethodSignature signature = (MethodSignature)pjp.getSignature();
        // 2.获取字节码
        Class<?> clazz = pjp.getTarget().getClass();
        // 3.方法名称
        String name = signature.getName();
        // 4.方法参数列表
        Class<?>[] parameterTypes = signature.getMethod().getParameterTypes();
        return tryGetDeclaredMethod(clazz, name, parameterTypes);
    }

    private Method tryGetDeclaredMethod(Class<?> clazz, String name, Class<?> ... parameterTypes){
        try {
            // 5.反射获取方法
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                // 尝试从父类寻找
                return tryGetDeclaredMethod(superClass, name, parameterTypes);
            }
        }
        return null;
    }
}