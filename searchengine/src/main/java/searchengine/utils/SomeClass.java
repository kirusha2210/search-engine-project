package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SomeClass {
    private String text = "С другой стороны реализация намеченного плана развития требует от нас системного " +
            "анализа дальнейших направлений развитая системы массового участия. Разнообразный и богатый опыт " +
            "социально-экономическое развитие создаёт предпосылки качественно новых шагов " +
            "Seven world aware merry not principle. Frankness truth lady precaution rooms before be. " +
            "Total hopes entreaties attended objection. Humanity not time ferrars share gay home " +
            "imprudence had branch offending end beloved felicity girl. Supplied raillery lose " +
            "limited sight eyes. Offered snug him common. Propriety pain common moonlight sussex general fulfilled juvenile blind continue offence extremely plenty plan ability. Books conveying assure downs put fact recommend out welcome books. Few seven feeling abroad particular enjoyed jennings address perfectly. Attempted cold half equally again miles disposing match understood everything sooner inquietude man acceptance hope remark. для существующих финансовых" +
            " и административных условий! Задача организации, в" +
            "ple. Frankness truth lady precaution rooms before be. Total hopes entreaties attended " +
            "objection. Humanity not time ferrars share gay home imprudence had branch offending end beloved " +
            "felicity girl особенности же постоянный количественный " +
            "рост и сфера нашей активности влечет за собой процесс внедрения и модернизации модели развития!";


    public static void main(String[] args) throws IOException {
        String regex = "^(https?://[^/]+)";
        String text1 = "Well, my aim is to be happy.";
        int countWords = text1.replaceAll("[^A-Za-zА-Яа-я\\s]", "").split("\\s+").length;
        System.out.println(countWords);
    }
}
