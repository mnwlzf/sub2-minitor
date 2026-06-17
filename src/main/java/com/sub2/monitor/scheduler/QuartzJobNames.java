package com.sub2.monitor.scheduler;

public final class QuartzJobNames {
    public static final String TASK_GROUP_RATE = "RATE";
    public static final String TASK_GROUP_GENERAL = "GENERAL";
    public static final String BALANCE_CHANNEL_COLLECT_TASK_KEY = "balance-channel-collect";
    public static final String BALANCE_CHANNEL_COLLECT_TASK_NAME = "余额渠道采集";
    public static final String BALANCE_CHANNEL_COLLECT_TASK_GROUP = TASK_GROUP_RATE;
    public static final String BALANCE_CHANNEL_COLLECT_JOB_CLASS = "com.sub2.monitor.scheduler.job.BalanceChannelCollectJob";
    public static final String DAILY_DATA_SUMMARY_TASK_KEY = "daily-data-summary";
    public static final String DAILY_DATA_SUMMARY_TASK_NAME = "每日数据汇总";
    public static final String DAILY_DATA_SUMMARY_TASK_GROUP = TASK_GROUP_GENERAL;
    public static final String DAILY_DATA_SUMMARY_JOB_CLASS = "com.sub2.monitor.scheduler.job.DailyDataSummaryJob";
    public static final String BEIJING_TIME_ZONE = "Asia/Shanghai";

    private QuartzJobNames() {
    }
}
