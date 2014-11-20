package com.github.abel533.mapper;

import org.apache.ibatis.builder.annotation.ProviderSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;

/**
 * Created by liuzh on 2014/11/19.
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class MapperInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] objects = invocation.getArgs();
        MappedStatement ms = (MappedStatement) objects[0];
        String msId = ms.getId();
        //不需要拦截的方法直接返回
        if (!MapperHelper.isMapperMethod(msId)) {
            return invocation.proceed();
        }
        //第一次经过处理后，就不会是ProviderSqlSource了，一开始高并发时可能会执行多次，但不影响。以后就不会在执行了
        if (ms.getSqlSource() instanceof ProviderSqlSource) {
            switch (ms.getSqlCommandType()) {
                case SELECT:
                    MapperHelper.selectSqlSource(ms);
                    break;
                case INSERT:
                    MapperHelper.insertSqlSource(ms);
                    break;
                case UPDATE:
                    MapperHelper.updateSqlSource(ms);
                    break;
                case DELETE:
                    MapperHelper.deleteSqlSource(ms);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
        //只有select和delete通过PK操作时需要处理入参
        MapperHelper.processParameterObject(ms,objects);
        Object result = invocation.proceed();
        return result;
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
