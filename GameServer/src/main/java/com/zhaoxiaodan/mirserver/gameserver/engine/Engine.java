package com.zhaoxiaodan.mirserver.gameserver.engine;

import com.zhaoxiaodan.mirserver.db.DB;
import com.zhaoxiaodan.mirserver.gameserver.entities.Config;
import com.zhaoxiaodan.mirserver.utils.NumUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import java.lang.reflect.Method;

public class Engine {

    private static final Logger logger = LogManager.getLogger();
    private static final Class[] engineClasses = new Class[]{
            CmdEngine.class,
            ScriptEngine.class,
            MessageEngine.class,
            MapEngine.class,
            MagicEngine.class,
            ItemEngine.class,
            MerchantEngine.class,
            MonsterEngine.class,
    };

    public static boolean running = true;

    public static synchronized void init() throws Exception {
        reload();
        startTickThread();
    }

    public static synchronized void reload() throws Exception {
        for (Class engineClazz : engineClasses) {
            String funName = "reload";
            try {
                logger.debug("引擎 {} 加载...", engineClazz.getSimpleName());

                Method reloadMethod = engineClazz.getDeclaredMethod(funName);

                try {
                    DB.getSession().getTransaction().begin();
                    reloadMethod.invoke(null);
                    if (DB.getSession().getTransaction().getStatus().isOneOf(TransactionStatus.ACTIVE))
                        DB.getSession().getTransaction().commit();
                } catch (Exception e) {
                    if (DB.getSession().isOpen())
                        DB.getSession().getTransaction().rollback();
                    logger.error("invoke reload error", e);
                    throw e;
                }


                logger.debug("引擎 {} 加载完成", engineClazz.getSimpleName());

            } catch (NoSuchMethodException e) {
                logger.error("引擎 {} 没有实现 {} 方法 !", engineClazz.getSimpleName(), funName);
                throw e;
            } catch (SecurityException e) {
                logger.error("引擎 {} 没有实现 {} 方法不是public !", engineClazz.getSimpleName(), funName);
                throw e;
            }
        }
    }

    private static synchronized void startTickThread() throws Exception {
        for (Class engineClazz : engineClasses) {
            String funName = "onTick";
            try {
                final Method onTickMethod = engineClazz.getDeclaredMethod(funName, long.class);
                new Thread() {
                    @Override
                    public void run() {
                        while (running) {
                            try {
                                DB.getSession().getTransaction().begin();
                                onTickMethod.invoke(null, NumUtil.getTickCount());
                                DB.getSession().getTransaction().commit();
                            } catch (Exception e) {
                                if (DB.getSession().isOpen())
                                    DB.getSession().getTransaction().rollback();
                                logger.error("invoke onTick error", e);
                            }

                            try {
                                Thread.sleep(Config.ENGINE_TICK_INTERVAL_TIME);
                            } catch (InterruptedException e) {
                                logger.error(e);
                            }
                        }
                    }
                }.start();
                logger.debug("引擎 {} onTick 启动完成", engineClazz.getSimpleName());

            } catch (NoSuchMethodException e) {
                logger.error("引擎 {} 没有实现 {} 方法 !", engineClazz.getSimpleName(), funName);
            } catch (SecurityException e) {
                logger.error("引擎 {} 没有实现 {} 方法不是public !", engineClazz.getSimpleName(), funName);
            }
        }
    }
}
