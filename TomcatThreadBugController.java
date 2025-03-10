
// bug记录：当前用户本地线程缓存的信息被另一个用户拿到了
@RestController
@RequestMapping("/tomcat-thread-bug")
public class TomcatThreadBugController {
    
    private static final ThreadLocal<Long> currentUser = ThreadLocal.withInitial(() -> null);

    @GetMapping("/get-user-info")
    public Map<String,String> getUserInfo(@RequestMapping Long userId){
        // 修改配置，模拟并发 server.tomcat.max-threads=1
        String before = Thread.currentThread().getName() + ":" +currentUser.get();
        currentUser.set(userId);
        String after = Thread.currentThread().getName() + ":" +currentUser.get();
        return Map.of("before", before, "after", after);
        // user1: {"before": "http-nio-49082-exec-1:null","after": "http-nio-49082-exec-1:23"}
        // user2: {"before":"http-nio-49082-exec-1:23","after":"http-nio-49082-exec-1:523"}

        // 原因：线程池会重用固定的几个线程，一旦线程重用，那么很可能首次从 ThreadLocal 获取的值是之前其他用户的请求遗留的值。
        // 这时，ThreadLocal 中的用户信息就是其他用户的信息。
        // 而Tomcat 的工作线程就是基于线程池的
    }

    @GetMapping("/get-user-info-clear")
    public Map<String,String> clearUserInfo(@RequestMapping Long userId){
        // 解决方案，当需要在 ThreadLocal 在代码运行后显示的清空缓存的数据
         String before = Thread.currentThread().getName() + ":" +currentUser.get();
         try {
            currentUser.set(userId);
            String after = Thread.currentThread().getName() + ":" +currentUser.get();
            return Map.of("before", before, "after", after);
            // user1: {"before":"http-nio-49082-exec-1:null","after":"http-nio-49082-exec-1:33"}
            // user2: {"before":"http-nio-49082-exec-1:null","after":"http-nio-49082-exec-1:82"}
         }finally {
            currentUser.remove();
         }
    }
}