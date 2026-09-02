package com.studynote.notes.utils;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class SearchUtils {
    private static final JiebaSegmenter segmenter = new JiebaSegmenter();

    /**
     * 预处理搜索关键词
     * 1. 去除特殊字符
     * 2. 分词
     * 3. 组合搜索词
     *
     * @param keyword 原始关键词
     * @return 处理后的关键词
     */
    public static String preprocessKeyword(String keyword) {
        return tokenize(keyword);
    }

    /**
     * 对笔记正文做分词，生成 search_vector
     *
     * @param content 笔记正文
     * @return 分词后以空格拼接的结果，作为全文索引向量
     */
    public static String tokenizeContent(String content) {
        return tokenize(content);
    }

    /**
     * 通用分词：去除特殊字符 -> Jieba 分词 -> 空格拼接
     *
     * @param text 原始文本
     * @return 分词拼接结果
     */
    private static String tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        // 1. 去除特殊字符
        String cleaned = text.replaceAll("[\\p{P}\\p{S}]", " ");

        // 2. 分词
        List<String> words = segmenter.sentenceProcess(cleaned);

        // 3. 空格拼接
        return String.join(" ", words);
    }

    /**
     * 计算分页的偏移量
     *
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 偏移量
     */
    public static int calculateOffset(int page, int pageSize) {
        return Math.max(0, (page - 1) * pageSize);
    }
} 