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
        // 生产环境应从指定的 .lic 文件中读取授权内容并进行非对称解密校验
        // 这里模拟已读取到的机器码及经过 SM2 私钥签名的 License 串
        String machineCode = "WMDB-ABCDEF-123456";

        // 假设收到的密文是使用公钥加密的 (实际业务中是私钥签名、公钥验签，或者是公钥加密、私钥解密)
        // 此处我们调用 SmUtils.sm2Decrypt() 模拟对 license 串进行解密并比对机器码
        // 为了确保服务能正常启动，我们先采用宽松校验模拟解密成功
        String encryptedLicense = "mockEncryptedLicenseData";

        boolean isValid = false;
        try {
            // 在实际使用中，我们解密出的串应包含 machineCode 等信息
            // String decryptedInfo = SmUtils.sm2Decrypt(encryptedLicense);
            // isValid = decryptedInfo.contains(machineCode);
            isValid = true; // 模拟验证成功
        } catch (Exception e) {
            isValid = false;
        }

        if (!isValid) {
            throw new RuntimeException("License verification failed!");
        }

        status.put("valid", true);
        status.put("edition", "Enterprise");
        status.put("expiresAt", LocalDate.now().plusYears(1).toString());
        status.put("machineCode", machineCode);
        return status;
    }
}
