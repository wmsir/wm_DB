<template>
  <div class="notification-config-container page-container">
    <div class="page-header-flex">
      <div>
        <h2 class="page-title">
          <span>🔔 消息通知与告警通道配置中心</span>
        </h2>
        <div class="page-subtitle">集中管理企业微信工作消息、阿里钉钉群、字节飞书机器人、紧急电话智能语音外呼及全局推送频次与静默策略</div>
      </div>
      <div class="header-actions">
        <el-button type="primary" :icon="Check" :loading="saving" @click="handleSaveConfig">
          保存全部配置
        </el-button>
        <el-button :icon="Refresh" :loading="loading" @click="fetchConfig">
          重置 / 刷新
        </el-button>
      </div>
    </div>

    <!-- 顶部通道概览胶囊 -->
    <el-row :gutter="14" class="channel-summary-row">
      <el-col :xs="24" :sm="6">
        <div class="channel-summary-card" :class="{ 'is-active': form.wechat?.enabled }" @click="activeTab = 'wechat'">
          <div class="channel-icon-wrap wechat-icon">🟢</div>
          <div class="channel-info">
            <div class="c-title">企业微信工作通知</div>
            <div class="c-desc">WebService / WSDL 消息总线</div>
          </div>
          <el-tag size="small" :type="form.wechat?.enabled ? 'success' : 'info'" effect="dark">
            {{ form.wechat?.enabled ? '已启用' : '已停用' }}
          </el-tag>
        </div>
      </el-col>
      <el-col :xs="24" :sm="6">
        <div class="channel-summary-card" :class="{ 'is-active': form.dingtalk?.enabled }" @click="activeTab = 'dingtalk'">
          <div class="channel-icon-wrap dingtalk-icon">🔵</div>
          <div class="channel-info">
            <div class="c-title">阿里钉钉群 / 通知</div>
            <div class="c-desc">Webhook 机器人与加签签名</div>
          </div>
          <el-tag size="small" :type="form.dingtalk?.enabled ? 'primary' : 'info'" effect="dark">
            {{ form.dingtalk?.enabled ? '已启用' : '已停用' }}
          </el-tag>
        </div>
      </el-col>
      <el-col :xs="24" :sm="6">
        <div class="channel-summary-card" :class="{ 'is-active': form.feishu?.enabled }" @click="activeTab = 'feishu'">
          <div class="channel-icon-wrap feishu-icon">🟣</div>
          <div class="channel-info">
            <div class="c-title">字节飞书 / Lark</div>
            <div class="c-desc">群机器人与富文本交互卡片</div>
          </div>
          <el-tag size="small" :type="form.feishu?.enabled ? 'warning' : 'info'" effect="dark">
            {{ form.feishu?.enabled ? '已启用' : '已停用' }}
          </el-tag>
        </div>
      </el-col>
      <el-col :xs="24" :sm="6">
        <div class="channel-summary-card" :class="{ 'is-active': form.voiceCall?.enabled }" @click="activeTab = 'voiceCall'">
          <div class="channel-icon-wrap voice-icon">📞</div>
          <div class="channel-info">
            <div class="c-title">紧急电话语音外呼</div>
            <div class="c-desc">P0 故障 / 严重失败电话直拨</div>
          </div>
          <el-tag size="small" :type="form.voiceCall?.enabled ? 'danger' : 'info'" effect="dark">
            {{ form.voiceCall?.enabled ? '已启用' : '已停用' }}
          </el-tag>
        </div>
      </el-col>
    </el-row>

    <!-- 主配置面板 -->
    <el-tabs v-model="activeTab" type="border-card" class="config-tabs">
      <!-- 1. 企业微信配置 -->
      <el-tab-pane name="wechat">
        <template #label>
          <span class="tab-label">🟢 企业微信应用工作消息 (WeChat Work)</span>
        </template>

        <div class="tab-content-grid">
          <div class="form-section">
            <el-form :model="form.wechat" label-position="top" label-width="140px">
              <el-form-item label="通道启用状态">
                <el-switch v-model="form.wechat.enabled" active-text="开启企微工作消息下发" inactive-text="关闭" />
              </el-form-item>

              <el-form-item label="集成协议模式">
                <el-radio-group v-model="form.wechat.mode">
                  <el-radio-button value="WEBSERVICE">WebService (WSDL / SOAP 接口)</el-radio-button>
                  <el-radio-button value="WEBHOOK">群机器人 Webhook</el-radio-button>
                </el-radio-group>
              </el-form-item>

              <template v-if="form.wechat.mode === 'WEBSERVICE'">
                <el-form-item label="WebService WSDL 地址">
                  <el-input
                    v-model="form.wechat.wsdlEndpoint"
                    placeholder="http://9.0.17.52:8083/wechat-wbs/services/ExternalDeptMessageService?wsdl"
                    clearable
                  />
                  <div class="form-tip">支持 WSDL 描述地址或 SOAP 服务端点 URL（系统会自动去除后缀兼容调用）</div>
                </el-form-item>

                <el-row :gutter="16">
                  <el-col :span="8">
                    <el-form-item label="系统接入标识 (sys_id)">
                      <el-input v-model="form.wechat.sysId" placeholder="例如 WMDB_SYSTEM" clearable />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="系统认证密码 (sys_id_pass)">
                      <el-input v-model="form.wechat.sysIdPass" show-password placeholder="请输入对接密码" clearable />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="系统标志 (sys_flag)">
                      <el-input v-model="form.wechat.sysFlag" placeholder="例如 1" clearable />
                    </el-form-item>
                  </el-col>
                </el-row>
              </template>

              <template v-else>
                <el-form-item label="企业微信群机器人 Webhook 地址">
                  <el-input
                    v-model="form.wechat.webhookUrl"
                    placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..."
                    clearable
                  />
                </el-form-item>
              </template>

              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="单用户推送频次限流 (条/分钟)">
                    <el-input-number v-model="form.wechat.frequencyLimit" :min="0" :max="300" style="width: 100%;" />
                    <div class="form-tip">限制对同一 ERP 账号每分钟最多下发的消息数，0 表示不限流</div>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="网络异常重试次数">
                    <el-input-number v-model="form.wechat.retryTimes" :min="0" :max="5" style="width: 100%;" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <!-- 在线测试诊断控制台 -->
          <div class="test-panel-card">
            <div class="test-panel-title">
              <span>🧪 企业微信通道在线检测 (Test Push)</span>
            </div>
            <div class="test-panel-desc">
              向指定的员工 ERP 账号 / 工号发送一条实时探测测试消息，验证 WebService 连通性与权限状态。
            </div>

            <div class="test-field-group">
              <div class="field-label">接收人 ERP 账号 / 员工工号：</div>
              <el-input
                v-model="wechatTestErp"
                placeholder="请输入员工 ERP / 企微 UserID (如 zhangsan, 01088234)"
                clearable
              >
                <template #prepend>ERP 账号</template>
              </el-input>
              <div class="erp-help-text">
                ⚠️ <b>重要提示</b>：此处必须填写员工的 <b>ERP 工号 / 登录账号 / 企微 UserID</b>，严禁填写中文姓名或 18 位身份证号码！
              </div>
            </div>

            <div class="test-field-group" style="margin-top: 12px;">
              <div class="field-label">自定义测试消息内容：</div>
              <el-input
                v-model="wechatTestMsg"
                type="textarea"
                :rows="3"
                placeholder="请输入测试推送内容..."
              />
            </div>

            <div class="test-actions">
              <el-button
                type="success"
                :icon="Promotion"
                :loading="testingWechat"
                style="width: 100%;"
                @click="handleTestChannel('WECHAT')"
              >
                立即发送企业微信测试消息
              </el-button>
            </div>

            <!-- 测试结果反馈 -->
            <div v-if="testResultMap.WECHAT" class="test-feedback-box" :class="{ 'is-ok': testResultMap.WECHAT.success, 'is-fail': !testResultMap.WECHAT.success }">
              <div class="feedback-status">
                <el-icon v-if="testResultMap.WECHAT.success" color="#67C23A"><CircleCheckFilled /></el-icon>
                <el-icon v-else color="#F56C6C"><CircleCloseFilled /></el-icon>
                <span>{{ testResultMap.WECHAT.success ? '✅ 推送成功' : '❌ 推送失败' }}</span>
                <span class="latency-tag" v-if="testResultMap.WECHAT.latencyMs">耗时: {{ testResultMap.WECHAT.latencyMs }}ms</span>
              </div>
              <div class="feedback-msg">{{ testResultMap.WECHAT.message }}</div>
              <div class="feedback-raw" v-if="testResultMap.WECHAT.rawResponse">
                <div class="raw-title">服务端原始响应：</div>
                <pre>{{ testResultMap.WECHAT.rawResponse }}</pre>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 2. 阿里钉钉配置 -->
      <el-tab-pane name="dingtalk">
        <template #label>
          <span class="tab-label">🔵 阿里钉钉群 / 工作通知 (DingTalk)</span>
        </template>

        <div class="tab-content-grid">
          <div class="form-section">
            <el-form :model="form.dingtalk" label-position="top" label-width="140px">
              <el-form-item label="通道启用状态">
                <el-switch v-model="form.dingtalk.enabled" active-text="开启钉钉群消息广播" inactive-text="关闭" />
              </el-form-item>

              <el-form-item label="钉钉自定义机器人 Webhook 地址">
                <el-input
                  v-model="form.dingtalk.webhookUrl"
                  placeholder="https://oapi.dingtalk.com/robot/send?access_token=..."
                  clearable
                />
                <div class="form-tip">在钉钉群设置中添加“自定义机器人”，并复制生成的 Webhook 地址</div>
              </el-form-item>

              <el-form-item label="安全设置加签秘钥 (Secret)">
                <el-input
                  v-model="form.dingtalk.secret"
                  show-password
                  placeholder="SEC... (可选，若机器人开启加签安全校验时必填)"
                  clearable
                />
                <div class="form-tip">系统将自动使用 HMAC-SHA256 算法计算时间戳与签名并安全附加至请求中</div>
              </el-form-item>

              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="高危报警是否 @所有人">
                    <el-switch v-model="form.dingtalk.atAll" active-text="是 (全部群成员)" inactive-text="否" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="群推送频次上限 (条/分钟)">
                    <el-input-number v-model="form.dingtalk.frequencyLimit" :min="1" :max="300" style="width: 100%;" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <!-- 在线测试钉钉 -->
          <div class="test-panel-card">
            <div class="test-panel-title">
              <span>🧪 钉钉机器人在线检测 (Test DingTalk)</span>
            </div>
            <div class="test-panel-desc">
              向配置的钉钉群发送 Markdown 测试卡片，检验 Webhook 连通性与加签签名算法是否匹配。
            </div>

            <div class="test-field-group">
              <div class="field-label">测试消息正文：</div>
              <el-input
                v-model="dingtalkTestMsg"
                type="textarea"
                :rows="4"
                placeholder="请输入要发送到钉钉群的测试内容..."
              />
            </div>

            <div class="test-actions" style="margin-top: 16px;">
              <el-button
                type="primary"
                :icon="Promotion"
                :loading="testingDingtalk"
                style="width: 100%;"
                @click="handleTestChannel('DINGTALK')"
              >
                立即发送钉钉群测试消息
              </el-button>
            </div>

            <!-- 测试结果反馈 -->
            <div v-if="testResultMap.DINGTALK" class="test-feedback-box" :class="{ 'is-ok': testResultMap.DINGTALK.success, 'is-fail': !testResultMap.DINGTALK.success }">
              <div class="feedback-status">
                <el-icon v-if="testResultMap.DINGTALK.success" color="#67C23A"><CircleCheckFilled /></el-icon>
                <el-icon v-else color="#F56C6C"><CircleCloseFilled /></el-icon>
                <span>{{ testResultMap.DINGTALK.success ? '✅ 发送成功' : '❌ 发送失败' }}</span>
                <span class="latency-tag" v-if="testResultMap.DINGTALK.latencyMs">耗时: {{ testResultMap.DINGTALK.latencyMs }}ms</span>
              </div>
              <div class="feedback-msg">{{ testResultMap.DINGTALK.message }}</div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 3. 字节飞书配置 -->
      <el-tab-pane name="feishu">
        <template #label>
          <span class="tab-label">🟣 字节飞书 / Lark (Feishu)</span>
        </template>

        <div class="tab-content-grid">
          <div class="form-section">
            <el-form :model="form.feishu" label-position="top" label-width="140px">
              <el-form-item label="通道启用状态">
                <el-switch v-model="form.feishu.enabled" active-text="开启飞书消息卡片推送" inactive-text="关闭" />
              </el-form-item>

              <el-form-item label="飞书自定义群机器人 Webhook 地址">
                <el-input
                  v-model="form.feishu.webhookUrl"
                  placeholder="https://open.feishu.cn/open-apis/bot/v2/hook/..."
                  clearable
                />
                <div class="form-tip">在飞书群设置 -> 群机器人 -> 添加机器人 -> 自定义机器人中获取 Webhook 地址</div>
              </el-form-item>

              <el-form-item label="安全设置签名校验密钥 (Secret)">
                <el-input
                  v-model="form.feishu.secret"
                  show-password
                  placeholder="请输入飞书机器人安全设置中的签名校验密钥 (可选)"
                  clearable
                />
                <div class="form-tip">若飞书机器人勾选了“签名校验”，系统会自动结合时间戳生成 HMAC-SHA256 签名进行安全验证</div>
              </el-form-item>

              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="群推送频次上限 (条/分钟)">
                    <el-input-number v-model="form.feishu.frequencyLimit" :min="1" :max="300" style="width: 100%;" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="消息卡片形式">
                    <el-tag size="default" type="warning" effect="plain" style="margin-top: 4px;">Interactive 交互式富文本卡片</el-tag>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <!-- 在线测试飞书 -->
          <div class="test-panel-card">
            <div class="test-panel-title">
              <span>🧪 飞书群机器人在线检测 (Test Feishu)</span>
            </div>
            <div class="test-panel-desc">
              向配置的飞书群发送 Interactive 交互式卡片测试消息，验证 Webhook 连通性与加签签名算法。
            </div>

            <div class="test-field-group">
              <div class="field-label">测试卡片正文：</div>
              <el-input
                v-model="feishuTestMsg"
                type="textarea"
                :rows="4"
                placeholder="请输入要发送到飞书群的测试内容..."
              />
            </div>

            <div class="test-actions" style="margin-top: 16px;">
              <el-button
                type="warning"
                :icon="Promotion"
                :loading="testingFeishu"
                style="width: 100%;"
                @click="handleTestChannel('FEISHU')"
              >
                立即发送飞书群测试卡片
              </el-button>
            </div>

            <!-- 测试结果反馈 -->
            <div v-if="testResultMap.FEISHU" class="test-feedback-box" :class="{ 'is-ok': testResultMap.FEISHU.success, 'is-fail': !testResultMap.FEISHU.success }">
              <div class="feedback-status">
                <el-icon v-if="testResultMap.FEISHU.success" color="#67C23A"><CircleCheckFilled /></el-icon>
                <el-icon v-else color="#F56C6C"><CircleCloseFilled /></el-icon>
                <span>{{ testResultMap.FEISHU.success ? '✅ 发送成功' : '❌ 发送失败' }}</span>
                <span class="latency-tag" v-if="testResultMap.FEISHU.latencyMs">耗时: {{ testResultMap.FEISHU.latencyMs }}ms</span>
              </div>
              <div class="feedback-msg">{{ testResultMap.FEISHU.message }}</div>
              <div class="feedback-raw" v-if="testResultMap.FEISHU.rawResponse">
                <div class="raw-title">服务端原始响应：</div>
                <pre>{{ testResultMap.FEISHU.rawResponse }}</pre>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 4. 紧急电话语音外呼配置 -->
      <el-tab-pane name="voiceCall">
        <template #label>
          <span class="tab-label">📞 紧急电话智能语音外呼 (Voice Call)</span>
        </template>

        <div class="tab-content-grid">
          <div class="form-section">
            <el-alert
              title="电话外呼功能定位：用于系统发生 P0 级别严重变更故障、数据库宕机或高危恶意 SQL 阻断时，直接向值班 DBA 与主管发起实时电话呼叫！"
              type="warning"
              :closable="false"
              show-icon
              style="margin-bottom: 16px;"
            />

            <el-form :model="form.voiceCall" label-position="top" label-width="140px">
              <el-form-item label="语音外呼功能启用">
                <el-switch v-model="form.voiceCall.enabled" active-text="开启电话语音外呼" inactive-text="关闭" />
              </el-form-item>

              <el-form-item label="语音外呼服务提供商">
                <el-radio-group v-model="form.voiceCall.provider">
                  <el-radio-button value="ALIYUN">阿里云语音服务 (SingleCallByTts)</el-radio-button>
                  <el-radio-button value="TENCENT">腾讯云语音通知</el-radio-button>
                  <el-radio-button value="SIP_GATEWAY">自建企业 SIP / IVR 语音网关</el-radio-button>
                </el-radio-group>
              </el-form-item>

              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="AccessKey ID / API Key">
                    <el-input v-model="form.voiceCall.accessKeyId" placeholder="LTAI..." clearable />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="AccessKey Secret">
                    <el-input v-model="form.voiceCall.accessKeySecret" show-password placeholder="请输入语音密钥" clearable />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="TTS 语音播报模板 Code">
                    <el-input v-model="form.voiceCall.templateCode" placeholder="例如 TTS_123456789" clearable />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="外呼主叫显示号码">
                    <el-input v-model="form.voiceCall.calledShowNumber" placeholder="例如 057188888888" clearable />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="默认紧急值班联系人手机号列表">
                <div v-for="(_, index) in form.voiceCall.emergencyContacts" :key="index" style="display: flex; gap: 8px; margin-bottom: 8px;">
                  <el-input v-model="form.voiceCall.emergencyContacts[index]" placeholder="请输入 11 位手机号码" style="max-width: 320px;" />
                  <el-button type="danger" plain :icon="Delete" @click="removeContact(Number(index))">删除</el-button>
                </div>
                <el-button type="primary" plain :icon="Plus" size="small" @click="addContact">添加紧急联系人号码</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 在线测试语音外呼 -->
          <div class="test-panel-card">
            <div class="test-panel-title">
              <span>🧪 电话语音外呼在线拨测 (Test Voice Call)</span>
            </div>
            <div class="test-panel-desc">
              输入被叫手机号码，发起一次自动化告警语音呼叫握手测试。
            </div>

            <div class="test-field-group">
              <div class="field-label">测试被叫手机号：</div>
              <el-input
                v-model="voiceTestPhone"
                placeholder="请输入 11 位手机号码 (如 13800138000)"
                clearable
              >
                <template #prepend>📱 手机号</template>
              </el-input>
            </div>

            <div class="test-actions" style="margin-top: 16px;">
              <el-button
                type="danger"
                :icon="Phone"
                :loading="testingVoice"
                style="width: 100%;"
                @click="handleTestChannel('VOICE_CALL')"
              >
                发起语音外呼拨测测试
              </el-button>
            </div>

            <!-- 测试结果反馈 -->
            <div v-if="testResultMap.VOICE_CALL" class="test-feedback-box" :class="{ 'is-ok': testResultMap.VOICE_CALL.success, 'is-fail': !testResultMap.VOICE_CALL.success }">
              <div class="feedback-status">
                <el-icon v-if="testResultMap.VOICE_CALL.success" color="#67C23A"><CircleCheckFilled /></el-icon>
                <el-icon v-else color="#F56C6C"><CircleCloseFilled /></el-icon>
                <span>{{ testResultMap.VOICE_CALL.success ? '✅ 外呼握手成功' : '❌ 外呼失败' }}</span>
                <span class="latency-tag" v-if="testResultMap.VOICE_CALL.latencyMs">耗时: {{ testResultMap.VOICE_CALL.latencyMs }}ms</span>
              </div>
              <div class="feedback-msg">{{ testResultMap.VOICE_CALL.message }}</div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 5. 推送频次与全局策略 -->
      <el-tab-pane name="policy">
        <template #label>
          <span class="tab-label">⚙️ 推送频次与全局触发策略 (Policies)</span>
        </template>

        <div style="max-width: 800px; padding: 12px 0;">
          <el-form :model="form.policy" label-position="left" label-width="280px">
            <el-divider content-position="left">工单生命周期事件触发开关</el-divider>
            <el-form-item label="工单创建/提交 (通知开发组长/DBA初审)">
              <el-switch v-model="form.policy.notifyOnSubmit" />
            </el-form-item>
            <el-form-item label="工单审批通过/驳回 (通知申请人)">
              <el-switch v-model="form.policy.notifyOnAudited" />
            </el-form-item>
            <el-form-item label="SQL 变更执行成功 (通知申请人与相关人)">
              <el-switch v-model="form.policy.notifyOnExecuted" />
            </el-form-item>
            <el-form-item label="SQL 变更执行失败 (高优报警通知)">
              <el-switch v-model="form.policy.notifyOnFailed" />
            </el-form-item>
            <el-form-item label="高危/恶意 SQL 拦截事件实时告警">
              <el-switch v-model="form.policy.notifyOnRiskIntercept" />
            </el-form-item>

            <el-divider content-position="left">夜间静默期设置 (防止非紧急消息打扰)</el-divider>
            <el-form-item label="开启夜间静默期控制">
              <el-switch v-model="form.policy.quietHoursEnabled" />
            </el-form-item>
            <el-form-item label="静默时段范围" v-if="form.policy.quietHoursEnabled">
              <div style="display: flex; gap: 8px; align-items: center;">
                <el-time-picker v-model="form.policy.quietHoursStart" value-format="HH:mm" placeholder="开始时间 (如 22:00)" />
                <span>至</span>
                <el-time-picker v-model="form.policy.quietHoursEnd" value-format="HH:mm" placeholder="结束时间 (如 08:00)" />
              </div>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Check,
  Refresh,
  Promotion,
  Plus,
  Delete,
  Phone,
  CircleCheckFilled,
  CircleCloseFilled
} from '@element-plus/icons-vue'
import request from '../utils/request'

const activeTab = ref('wechat')
const loading = ref(false)
const saving = ref(false)

const testingWechat = ref(false)
const testingDingtalk = ref(false)
const testingFeishu = ref(false)
const testingVoice = ref(false)

const wechatTestErp = ref('zhangsan')
const wechatTestMsg = ref('【wmDB 工单变更测试】您的 SQL 工单 #1024 已审批通过，预计将在维护窗口自动执行。')

const dingtalkTestMsg = ref('【wmDB 数据库告警通知】生产库 `rm-uf6ab...` 当前存在 2 个慢查询阻塞会话，请 DBA 及时关注。')
const feishuTestMsg = ref('【wmDB 飞书通知测试】工单 #1024 预执行 Dry-Run 校验通过，影响行数：12 行。')
const voiceTestPhone = ref('13800138000')

const testResultMap = reactive<Record<string, any>>({
  WECHAT: null,
  DINGTALK: null,
  FEISHU: null,
  VOICE_CALL: null
})

const form = reactive<any>({
  wechat: {
    enabled: true,
    mode: 'WEBSERVICE',
    wsdlEndpoint: 'http://9.0.17.52:8083/wechat-wbs/services/ExternalDeptMessageService?wsdl',
    sysId: 'WMDB_SYSTEM',
    sysIdPass: 'wmdb_pass_123',
    sysFlag: '1',
    webhookUrl: '',
    retryTimes: 3,
    frequencyLimit: 30
  },
  dingtalk: {
    enabled: true,
    webhookUrl: 'https://oapi.dingtalk.com/robot/send?access_token=your_token',
    secret: 'SEC_your_secret',
    atAll: false,
    frequencyLimit: 60
  },
  feishu: {
    enabled: true,
    mode: 'WEBHOOK',
    webhookUrl: 'https://open.feishu.cn/open-apis/bot/v2/hook/your_feishu_token',
    secret: '',
    appId: '',
    appSecret: '',
    frequencyLimit: 60
  },
  voiceCall: {
    enabled: false,
    provider: 'ALIYUN',
    endpoint: 'dyvmsapi.aliyuncs.com',
    accessKeyId: 'LTAI_your_key',
    accessKeySecret: 'your_secret',
    templateCode: 'TTS_123456',
    calledShowNumber: '057188888888',
    emergencyContacts: ['13800138000'],
    triggerEvents: ['TICKET_FAILED_P0', 'INSTANCE_DOWN']
  },
  policy: {
    notifyOnSubmit: true,
    notifyOnAudited: true,
    notifyOnExecuted: true,
    notifyOnFailed: true,
    notifyOnRiskIntercept: true,
    quietHoursEnabled: false,
    quietHoursStart: '22:00',
    quietHoursEnd: '08:00'
  }
})

const fetchConfig = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/v1/notification/config')
    if (res.data) {
      if (res.data.wechat) Object.assign(form.wechat, res.data.wechat)
      if (res.data.dingtalk) Object.assign(form.dingtalk, res.data.dingtalk)
      if (res.data.feishu) Object.assign(form.feishu, res.data.feishu)
      if (res.data.voiceCall) {
        Object.assign(form.voiceCall, res.data.voiceCall)
        if (!form.voiceCall.emergencyContacts) form.voiceCall.emergencyContacts = ['13800138000']
      }
      if (res.data.policy) Object.assign(form.policy, res.data.policy)
    }
  } catch (err: any) {
    ElMessage.error('加载通知通道配置失败: ' + (err.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleSaveConfig = async () => {
  saving.value = true
  try {
    await request.post('/v1/notification/config', form)
    ElMessage.success('消息通知与告警配置已成功保存并实时生效！')
    await fetchConfig()
  } catch (err: any) {
    ElMessage.error('保存失败: ' + (err.response?.data?.message || err.message))
  } finally {
    saving.value = false
  }
}

const handleTestChannel = async (channel: string) => {
  let target = ''
  let message = ''

  if (channel === 'WECHAT') {
    if (!wechatTestErp.value.trim()) {
      ElMessage.warning('请输入测试接收人 ERP 账号 / 工号')
      return
    }
    target = wechatTestErp.value.trim()
    message = wechatTestMsg.value
    testingWechat.value = true
  } else if (channel === 'DINGTALK') {
    message = dingtalkTestMsg.value
    testingDingtalk.value = true
  } else if (channel === 'FEISHU') {
    message = feishuTestMsg.value
    testingFeishu.value = true
  } else if (channel === 'VOICE_CALL') {
    if (!voiceTestPhone.value.trim()) {
      ElMessage.warning('请输入测试被叫手机号')
      return
    }
    target = voiceTestPhone.value.trim()
    testingVoice.value = true
  }

  try {
    const res: any = await request.post('/v1/notification/test-channel', {
      channel,
      target,
      message
    })
    testResultMap[channel] = res.data
    if (res.data?.success) {
      ElMessage.success(res.data.message || '通道测试通过！')
    } else {
      ElMessage.error(res.data.message || '通道测试返回失败')
    }
  } catch (err: any) {
    ElMessage.error('测试请求异常: ' + (err.response?.data?.message || err.message))
  } finally {
    testingWechat.value = false
    testingDingtalk.value = false
    testingFeishu.value = false
    testingVoice.value = false
  }
}

const addContact = () => {
  if (!form.voiceCall.emergencyContacts) {
    form.voiceCall.emergencyContacts = []
  }
  form.voiceCall.emergencyContacts.push('')
}

const removeContact = (index: number) => {
  form.voiceCall.emergencyContacts.splice(index, 1)
}

onMounted(() => {
  fetchConfig()
})
</script>

<style scoped>
.notification-config-container {
  width: 100%;
  box-sizing: border-box;
}

.page-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  flex-wrap: wrap;
  gap: 12px;
}

.channel-summary-row {
  margin-bottom: 20px;
}

.channel-summary-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.channel-summary-card:hover {
  border-color: #3b82f6;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.1);
}

.channel-summary-card.is-active {
  border-left: 4px solid #3b82f6;
}

.channel-icon-wrap {
  font-size: 24px;
}

.channel-info {
  flex: 1;
}

.c-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.c-desc {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.config-tabs {
  border-radius: 8px;
  background: #ffffff;
}

.tab-label {
  font-size: 14px;
  font-weight: 600;
}

.tab-content-grid {
  display: grid;
  grid-template-columns: 1fr 420px;
  gap: 24px;
  padding: 12px 4px;
}

@media (max-width: 1024px) {
  .tab-content-grid {
    grid-template-columns: 1fr;
  }
}

.form-section {
  padding-right: 12px;
}

.form-tip {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
  line-height: 1.4;
}

.erp-help-text {
  font-size: 12px;
  color: #d97706;
  background: #fffbeb;
  border: 1px solid #fef3c7;
  padding: 6px 10px;
  border-radius: 4px;
  margin-top: 6px;
  line-height: 1.4;
}

/* 在线测试诊断卡片 */
.test-panel-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
}

.test-panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 6px;
}

.test-panel-desc {
  font-size: 12.5px;
  color: #64748b;
  margin-bottom: 16px;
  line-height: 1.5;
}

.test-field-group {
  margin-bottom: 12px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 6px;
}

.test-actions {
  margin-top: 12px;
}

.test-feedback-box {
  margin-top: 16px;
  border-radius: 6px;
  padding: 12px 14px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
}

.test-feedback-box.is-ok {
  border-color: #67c23a;
  background: #f0f9eb;
}

.test-feedback-box.is-fail {
  border-color: #f56c6c;
  background: #fef0f0;
}

.feedback-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
  font-size: 13.5px;
  margin-bottom: 6px;
}

.latency-tag {
  margin-left: auto;
  font-size: 11px;
  color: #64748b;
  font-weight: normal;
}

.feedback-msg {
  font-size: 12.5px;
  color: #334155;
  line-height: 1.5;
}

.feedback-raw {
  margin-top: 10px;
  border-top: 1px dashed #cbd5e1;
  padding-top: 8px;
}

.raw-title {
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
}

.feedback-raw pre {
  margin: 4px 0 0 0;
  font-family: monospace;
  font-size: 11px;
  color: #475569;
  background: #ffffff;
  padding: 8px;
  border-radius: 4px;
  overflow-x: auto;
  max-height: 140px;
}
</style>
