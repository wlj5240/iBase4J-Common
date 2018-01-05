/**
 * 
 */
package top.ibase4j.core.util;

import java.io.File;

/**
 * 
 * @author ShenHuaJie
 * @version 2018年1月4日 下午6:59:34
 */
public class PathUtil {
    public static String getContainerName() {
        String dir = System.getProperty("catalina.home");
        dir = new File(dir == null ? System.getProperty("user.dir") : dir).getName();
        String name = new File(ClassLoader.getSystemResource("").getFile()).getParentFile().getParentFile().getName();
        if (name.equals(dir)) {
            return "main";
        }
        return "." + dir;
    }
}
