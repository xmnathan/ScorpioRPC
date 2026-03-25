package scorpio.core.rpc.common.function;

/**
 * 单参数函数接口
 */
@FunctionalInterface
public interface ScoFunction1<T1> {
    void apply(T1 t1);
}
