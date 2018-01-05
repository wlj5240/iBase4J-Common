/**
 * 
 */
package top.ibase4j.core.listener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * 
 * @author ShenHuaJie
 * @version 2018年1月5日 上午9:46:04
 */
public class ApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {
    protected final Logger logger = LogManager.getLogger();

    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("=================================");
        String server = event.getSpringApplication().getSources().iterator().next().toString();
        logger.info("系统[{}]启动完成!!!", server.substring(server.lastIndexOf(".") + 1));
        logger.info("=================================");
    }
}
