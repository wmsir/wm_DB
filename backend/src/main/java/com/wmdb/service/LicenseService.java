package com.wmdb.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * License 服务
 * <p>
 * 处理基于机器码或固定证书逻辑的商业授权状态（Mock）。
 * </p>
 *
 * @author wm
 * @date 2023-10-25
 */
@Service
public class LicenseService {

    /**
     * 获取商业授权状态
     */
    public Map<String, Object> checkLicenseStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("valid", true);
        status.put("edition", "Enterprise");
        status.put("expiresAt", LocalDate.now().plusYears(1).toString());
        status.put("machineCode", "WMDB-ABCDEF-123456");
        return status;
    }
}
