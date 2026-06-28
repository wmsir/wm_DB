package com.wmdb.runner;

import com.wmdb.service.LicenseService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * License 检查启动器
 * <p>
 * 在应用启动时校验商业授权，如果不通过则中断启动。
 * </p>
 *
 * @author wm
 */
@Component
public class LicenseCheckRunner implements CommandLineRunner {

    private final LicenseService licenseService;

    public LicenseCheckRunner(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("====== 正在校验系统 License ======");
        try {
            licenseService.checkLicenseStatus();
            System.out.println("====== License 校验通过 ======");
        } catch (Exception e) {
            System.err.println("====== License 校验失败，系统即将退出 ======");
            System.exit(1);
        }
    }
}
