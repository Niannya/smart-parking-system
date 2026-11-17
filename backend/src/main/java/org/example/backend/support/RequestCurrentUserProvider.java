package org.example.backend.support;

import org.example.backend.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Locale;

/**
 * 默认的当前用户提供者：读取鉴权层写入的请求属性。
 *
 * <p>本类不解析 Cookie、不信任任何请求头或请求体；没有认证结果时一律返回 1003 未登录。
 * 成员 1 的 Spring Security 合并后，由认证过滤器写入
 * {@link #ATTRIBUTE_NAME} 对应的 {@link CurrentUserView} 即可，无需改动成员 2 的业务代码。</p>
 *
 * <p>本地联调开关：{@code parking.dev.current-user-id} 与 {@code parking.dev.current-user-role}
 * 仅用于没有鉴权底座时跑通页面，默认关闭（id=0）。生产/验收环境不得设置。</p>
 */
@Component
public class RequestCurrentUserProvider implements CurrentUserProvider {

    /** 鉴权层写入请求属性的固定 key。 */
    public static final String ATTRIBUTE_NAME = "org.example.backend.support.CURRENT_USER";

    private static final Logger log = LoggerFactory.getLogger(RequestCurrentUserProvider.class);

    private final long devUserId;
    private final String devRole;

    public RequestCurrentUserProvider(
            @Value("${parking.dev.current-user-id:0}") long devUserId,
            @Value("${parking.dev.current-user-role:}") String devRole) {
        this.devUserId = devUserId;
        this.devRole = devRole == null ? "" : devRole.trim().toUpperCase(Locale.ROOT);
        if (this.devUserId > 0) {
            log.warn("已启用本地联调身份 parking.dev.current-user-id={}, role={}；"
                    + "该开关仅供无鉴权底座时调试，验收环境必须关闭。", this.devUserId, this.devRole);
        }
    }

    @Override
    public CurrentUserView currentUser() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            Object value = attributes.getAttribute(ATTRIBUTE_NAME, RequestAttributes.SCOPE_REQUEST);
            if (value instanceof CurrentUserView view) {
                return view;
            }
        }
        if (devUserId > 0 && isKnownRole(devRole)) {
            return new CurrentUserView(devUserId, devRole);
        }
        throw new BusinessException(1003, "未登录或登录已失效，请重新登录");
    }

    private boolean isKnownRole(String role) {
        return CurrentUserView.ROLE_ADMIN.equals(role) || CurrentUserView.ROLE_OWNER.equals(role);
    }
}
