package scorpio.core.rpc.common.function;

/**
 * 三参数函数接口
 */
@FunctionalInterface
public interface ScoFunction3<T1, T2, T3> {
    void apply(T1 t1, T2 t2, T3 t3);
}
