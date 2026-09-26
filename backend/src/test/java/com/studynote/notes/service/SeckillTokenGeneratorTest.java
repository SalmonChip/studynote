package com.studynote.notes.service;

import com.studynote.notes.utils.JwtUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 临时工具类，压测完就删掉。
 *
 * 生成一批测试用 token，供 Apifox / 脚本并发压测。
 * 可以直接用不存在的用户 ID（900001~900050）：JWT 是自签的（HS512 + secret），
 * TokenInterceptor 只验签名不查库，且 seckill_order.user_id 没有外键约束。
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class SeckillTokenGeneratorTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    public void generateTokens() {
        for (long userId = 900001; userId <= 900050; userId++) {
            String token = jwtUtil.generateToken(userId);
            // 输出格式：userId<TAB>token   —— 方便直接存成文件给脚本用
            System.out.println(userId + "\t" + token);
        }
    }
}
