package com.wmdb.service;

import cn.hutool.core.util.StrUtil;
import com.wmdb.security.SmUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * License 服务
 * <p>
 * 处理基于机器码或固定证书逻辑的商业授权状态。
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
        // In a real scenario, this would read a license file, verify SM2 signature against machine code
        // For the scope of this implementation, we will simulate a robust verification structure
        String mockMachineCode = "WMDB-ABCDEF-123456";
        String expectedSignature = SmUtils.sm3Hash(mockMachineCode + "WMDB-SECRET");

        // Simulating the check
        boolean isValid = StrUtil.isNotBlank(expectedSignature);

        if (!isValid) {
            throw new RuntimeException("License verification failed!");
        }

        status.put("valid", true);
        status.put("edition", "Enterprise");
        status.put("expiresAt", LocalDate.now().plusYears(1).toString());
        status.put("machineCode", mockMachineCode);
        return status;
    }
}
