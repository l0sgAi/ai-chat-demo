package com.losgai.ai.common.satoken;

import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 自定义侦听器的实现 
 */
@Component
@Slf4j
public class MySaTokenListener implements SaTokenListener {

    /** 每次登录时触发 */
    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginParameter loginParameter) {
        log.info("========>用户id:{} | 上线:",loginId);
    }

    /** 每次注销时触发 */
    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        log.info("========>用户id:{} | 登出:",loginId);
    }

    /** 每次被踢下线时触发 */
    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        log.info("========>用户id:{} | 被踢下线:",loginId);
    }

    /** 每次被顶下线时触发 */
    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        log.info("========>用户id:{} | 被顶下线:",loginId);

    }

    /** 每次被封禁时触发 */
    @Override
    public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
        log.info("========>用户id:{} | 被封禁:",loginId);

    }

    /** 每次被解封时触发 */
    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
        log.info("========>用户id:{} | 被解封:",loginId);
    }

    /** 每次二级认证时触发 */
    @Override
    public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {
        log.info("========>服务:{} | 二级认证:",service);
    }

    /** 每次退出二级认证时触发 */
    @Override
    public void doCloseSafe(String loginType, String tokenValue, String service) {
        log.info("========>服务:{} | 二级认证退出:",service);

    }

    /** 每次创建Session时触发 */
    @Override
    public void doCreateSession(String id) {
        log.info("========>Session:{} | 创建:",id);

    }

    /** 每次注销Session时触发 */
    @Override
    public void doLogoutSession(String id) {
        log.info("========>Session:{} | 注销:",id);
    }

    
    /** 每次Token续期时触发 */
    @Override
    public void doRenewTimeout(String loginType, Object loginId, String tokenValue, long timeout) {
        log.info("========>用户id:{} | token续期:",loginId);
    }

}
