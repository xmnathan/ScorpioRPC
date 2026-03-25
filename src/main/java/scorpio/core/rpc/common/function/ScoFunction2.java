package scorpio.core.rpc.common.function;

/**
 * 双参数函数接口
 */
@FunctionalInterface
public interface ScoFunction2<T1, T2> {
    void apply(T1 t1, T2 t2);
}
