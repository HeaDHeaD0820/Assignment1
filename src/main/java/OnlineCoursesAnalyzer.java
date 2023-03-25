import java.awt.image.AreaAveragingScaleFilter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * This is just a demo for you, please run it on JDK17 (some statements may be not allowed in lower version).
 * This is just a demo, and you can extend and implement functions
 * based on this demo, or implement it in a different way.
 */
public class OnlineCoursesAnalyzer {

    public static void main(String[] args) {
        OnlineCoursesAnalyzer myAnalyzer=new OnlineCoursesAnalyzer("src/main/resources/local.csv");
        // 1
//        System.out.println(mapToString(myAnalyzer.getPtcpCountByInst()));
        //2
//        System.out.println(mapToString(myAnalyzer.getPtcpCountByInstAndSubject()));
        //3
//        System.out.println(mapToString(myAnalyzer.getCourseListOfInstructor()));
        //4
//        System.out.println(listToString(myAnalyzer.getCourses(10,"hours")));
//        System.out.println(listToString(myAnalyzer.getCourses(15,"participants")));

        //5
        System.out.println(listToString(myAnalyzer.searchCourses("computer",20.0,700)));
        //6
//        System.out.println(mapToString(myAnalyzer.getCourseListOfInstructor()));

    }
    // NEED TO BE REMOVED BELOW

    static <K, V> String mapToString(Object obj) {
        Map<K, V> map = (Map<K, V>) obj;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            sb.append(entry.getKey());
            sb.append(" == ");
            sb.append(entry.getValue());
            sb.append("\n");
        }
        if (sb.length() == 0) return "";
        return sb.substring(0, sb.length() - 1).strip();
    }

    static String listToString(Object obj) {
        List<String> list = (List<String>) obj;
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s);
            sb.append("\n");
        }
        if (sb.length() == 0) return "";
        return sb.substring(0, sb.length() - 1).strip();
    }

    //// NEED TO BE REMOVED ABOVE

    List<Course> courses = new ArrayList<>();

    public OnlineCoursesAnalyzer(String datasetPath) {
        BufferedReader br = null;
        String line;
        try {
            br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
            br.readLine();
            while ((line = br.readLine()) != null) {
                    String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                Course course = new Course(info[0], info[1], new Date(info[2]), info[3], info[4], info[5],
                        Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
                        Integer.parseInt(info[9]), Integer.parseInt(info[10]), Double.parseDouble(info[11]),
                        Double.parseDouble(info[12]), Double.parseDouble(info[13]), Double.parseDouble(info[14]),
                        Double.parseDouble(info[15]), Double.parseDouble(info[16]), Double.parseDouble(info[17]),
                        Double.parseDouble(info[18]), Double.parseDouble(info[19]), Double.parseDouble(info[20]),
                        Double.parseDouble(info[21]), Double.parseDouble(info[22]));
                courses.add(course);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //1
    public Map<String, Integer> getPtcpCountByInst() {
        Stream<Course> stream = this.courses.stream();
        Map<String, Integer> result = stream.collect(
                Collectors.groupingBy(course->course.institution, Collectors.summingInt(course->course.participants)));
        return result;
    }

    //2
    public Map<String, Integer> getPtcpCountByInstAndSubject() {
        Stream<Course> stream = this.courses.stream();
        Map<String, Integer> map = stream.collect(Collectors.groupingBy(course->course.institution+"-"+course.subject, Collectors.summingInt(course->course.participants)));
        Stream<Map.Entry<String,Integer>> mapStream = map.entrySet().stream();
        Comparator<Map.Entry<String,Integer>> myComparator = (c1,c2) -> {
            if(c1.getValue().equals(c2.getValue())){
                return c1.getKey().compareTo(c2.getKey());
            }else{
                return -c1.getValue().compareTo(c2.getValue());
            }
        };
        LinkedHashMap<String, Integer> result = mapStream.sorted(myComparator).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2)->v1, LinkedHashMap::new));
        return result;
    }

    //3
    public Map<String, List<List<String>>> getCourseListOfInstructor() {
        Stream<Course> stream = this.courses.stream();
        // Get Instructor Set
        Set<String> instructors_set = new HashSet<>();
        stream.forEach(course -> {
            List<String> instructor_list = Arrays.asList(course.instructors.split(", "));
            instructors_set.addAll(instructor_list);
        });
        // instructor -> courses
        // for each instructor, check each course to see if the instructors(split) include the instructor, then add the course or not
        Map<String, List<Course>> instructorToCourses = instructors_set.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        cur_instructor -> this.courses.stream().filter(course -> Arrays.asList(course.instructors.split(", ")).contains(cur_instructor)).collect(Collectors.toList())
                ));
        // instructor -> [ [single_courses][coop_courses] ]
        Map<String, List<List<String>>> instructorToCoursesInTwoList = instructorToCourses.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            List<String> single_courses = instructorToCourses.get(e.getKey()).stream().filter(course->course.instructors.split(", ").length==1).map(course->course.title).distinct().sorted().toList();//.stream().filter(course -> );
                            List<String> coop_courses = instructorToCourses.get(e.getKey()).stream().filter(course->course.instructors.split(", ").length>1).map(course->course.title).distinct().sorted().toList();;
                            return Arrays.asList(single_courses,coop_courses);
                        }
                ));
        return instructorToCoursesInTwoList;
    }
    //4
    public List<String> getCourses(int topK, String by) {
        Stream<Course> stream = this.courses.stream();
        List<String> result = null;
        Comparator<Course> myComparator = null;
        if(by.equals("hours")){
            myComparator = (c1,c2) -> {
                if(c1.totalHours > c2.totalHours){
                    return -1;
                }else if(c1.totalHours < c2.totalHours){
                    return 1;
                }
                return 0;
            };
        }
        else if(by.equals("participants")){
            myComparator = (c1,c2) -> {
                if(c1.participants > c2.participants){
                    return -1;
                }else if(c1.participants < c2.participants){
                    return 1;
                }
                return 0;
            };
        }
        result = stream.sorted(myComparator).map(course->course.title).distinct().limit(topK).toList();
        return result;
    }
    //5
    public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
        Stream<Course> stream = this.courses.stream();
        List<String> result = stream.filter(course -> course.percentAudited>=percentAudited && course.totalHours<=totalCourseHours
        && course.subject.toLowerCase().contains(courseSubject.toLowerCase())).map(course -> course.title).sorted().distinct().toList();
        return result;
    }

    //6
    public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
        List<String> result = null;
        return null;
    }
}

class Course {
    String institution;
    String number;
    Date launchDate;
    String title;
    String instructors;
    String subject;
    int year;
    int honorCode;
    int participants;
    int audited;
    int certified;
    double percentAudited;
    double percentCertified;
    double percentCertified50;
    double percentVideo;
    double percentForum;
    double gradeHigherZero;
    double totalHours;
    double medianHoursCertification;
    double medianAge;
    double percentMale;
    double percentFemale;
    double percentDegree;

    public Course(String institution, String number, Date launchDate,
                  String title, String instructors, String subject,
                  int year, int honorCode, int participants,
                  int audited, int certified, double percentAudited,
                  double percentCertified, double percentCertified50,
                  double percentVideo, double percentForum, double gradeHigherZero,
                  double totalHours, double medianHoursCertification,
                  double medianAge, double percentMale, double percentFemale,
                  double percentDegree) {
        this.institution = institution;
        this.number = number;
        this.launchDate = launchDate;
        if (title.startsWith("\"")) title = title.substring(1);
        if (title.endsWith("\"")) title = title.substring(0, title.length() - 1);
        this.title = title;
        if (instructors.startsWith("\"")) instructors = instructors.substring(1);
        if (instructors.endsWith("\"")) instructors = instructors.substring(0, instructors.length() - 1);
        this.instructors = instructors;
        if (subject.startsWith("\"")) subject = subject.substring(1);
        if (subject.endsWith("\"")) subject = subject.substring(0, subject.length() - 1);
        this.subject = subject;
        this.year = year;
        this.honorCode = honorCode;
        this.participants = participants;
        this.audited = audited;
        this.certified = certified;
        this.percentAudited = percentAudited;
        this.percentCertified = percentCertified;
        this.percentCertified50 = percentCertified50;
        this.percentVideo = percentVideo;
        this.percentForum = percentForum;
        this.gradeHigherZero = gradeHigherZero;
        this.totalHours = totalHours;
        this.medianHoursCertification = medianHoursCertification;
        this.medianAge = medianAge;
        this.percentMale = percentMale;
        this.percentFemale = percentFemale;
        this.percentDegree = percentDegree;
    }
}