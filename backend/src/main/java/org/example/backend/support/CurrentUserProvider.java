package org.example.backend.support;

/**
 * 当前登录用户的提供者（成员 2 模块的端口）。
 *
 * <p>实现约定：未登录时必须抛 {@code BusinessException(1003)}，角色不符由调用方判断后抛
 * {@code BusinessException(1004)}。身份只能来自服务端会话，任何请求参数都不能影响结果。</p>
 *
 * <p>与成员 1 的对接方式（二选一）：</p>
 * <ol>
 *   <li>在鉴权过滤器/拦截器里执行
 *       {@code request.setAttribute(RequestCurrentUserProvider.ATTRIBUTE_NAME, new CurrentUserView(id, role))}；</li>
 *   <li>由成员 1 定义一个 {@code @Primary} 的 {@link CurrentUserProvider} Bean，把
 *       {@code common.CurrentUser} 的 {@code id()/role()} 转成 {@link CurrentUserView}。</li>
 * </ol>
 */
public interface CurrentUserProvider {

    CurrentUserView currentUser();
}
