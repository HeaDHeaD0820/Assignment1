import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;





/**
 *
 * <p>This is just a demo for you, please run it on JDK17 (some statements
 * may be not allowed in lower version).
 * This is just a demo, and you can extend and implement functions
 * based on this demo, or implement it in a different way.</p>
 */
public class OnlineCoursesAnalyzer {
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
            Collectors.groupingBy(course -> course.institution, 
                                  Collectors.summingInt(course -> course.participants)));
    return result;
  }

  //2
  public Map<String, Integer> getPtcpCountByInstAndSubject() {
    Stream<Course> stream = this.courses.stream();
    Map<String, Integer> map = stream.collect(Collectors.groupingBy(
            course -> course.institution + "-" + course.subject,
            Collectors.summingInt(course -> course.participants)));
    Stream<Map.Entry<String, Integer>> mapStream = map.entrySet().stream();
    Comparator<Map.Entry<String, Integer>> myComparator = (c1, c2) -> {
      if (c1.getValue().equals(c2.getValue())) {
        return c1.getKey().compareTo(c2.getKey());
      } else {
        return -c1.getValue().compareTo(c2.getValue());
      }
    };
    LinkedHashMap<String, Integer> result = mapStream.sorted(myComparator)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                      (v1, v2) -> v1, LinkedHashMap::new));
    return result;
  }

  //3
  public Map<String, List<List<String>>> getCourseListOfInstructor() {
    Stream<Course> stream = this.courses.stream();
    // Get Instructor Set
    Set<String> instructorSet = new HashSet<>();
    stream.forEach(course -> {
      List<String> instructorList = Arrays.asList(course.instructors.split(", "));
      instructorSet.addAll(instructorList);
    });
    // instructor -> courses
    // for each instructor, check each course to see if the instructors(split)
    // include the instructor, then add the course or not
    Map<String, List<Course>> instructorToCourses = instructorSet.stream()
            .collect(Collectors.toMap(
                    Function.identity(),
                    curInstructor -> this.courses.stream()
                            .filter(course -> Arrays.asList(course.instructors.split(", "))
                                                    .contains(curInstructor))
                            .collect(Collectors.toList())
            ));
    // instructor -> [ [single_courses][coop_courses] ]
    Map<String, List<List<String>>> instructorToCoursesInTwoList = instructorToCourses
            .entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> {
                      List<String> singleCourses = instructorToCourses.get(e.getKey()).stream()
                              .filter(course -> course.instructors.split(", ").length == 1)
                              .map(course -> course.title).distinct().sorted().toList();
                      List<String> coopCourses = instructorToCourses.get(e.getKey()).stream()
                              .filter(course -> course.instructors.split(", ").length > 1)
                              .map(course -> course.title).distinct().sorted().toList();;
                      return Arrays.asList(singleCourses, coopCourses);
                    }
            ));
    return instructorToCoursesInTwoList;
  }

  //4
  public List<String> getCourses(int topK, String by) {
    Stream<Course> stream = this.courses.stream();
    List<String> result = null;
    Comparator<Course> myComparator = null;
    if (by.equals("hours")) {
      myComparator = (c1, c2) -> {
        if (c1.totalHours > c2.totalHours) {
          return -1;
        } else if (c1.totalHours < c2.totalHours) {
          return 1;
        }
        return 0;
      };
    } else if (by.equals("participants")) {
      myComparator = (c1, c2) -> {
        if (c1.participants > c2.participants) {
          return -1;
        } else if (c1.participants < c2.participants) {
          return 1;
        }
        return 0;
      };
    }
    result = stream.sorted(myComparator).map(course -> course.title)
                   .distinct().limit(topK).toList();
    return result;
  }

  //5
  public List<String> searchCourses(String courseSubject,
                                    double percentAudited, double totalCourseHours) {
    Stream<Course> stream = this.courses.stream();
    List<String> result = stream.filter(course -> course.percentAudited >= percentAudited
                                        && course.totalHours <= totalCourseHours
                                        && course.subject.toLowerCase()
                                                 .contains(courseSubject.toLowerCase()))
                                        .map(course -> course.title).sorted().distinct().toList();
    return result;
  }

  //6
  public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
    Map<String, Double> mapToAge = courses.stream()
            .collect(Collectors.groupingBy(course -> course.number,
                     Collectors.averagingDouble(course -> course.medianAge)));
    Map<String, Double> mapToGen = courses.stream()
            .collect(Collectors.groupingBy(course -> course.number,
                     Collectors.averagingDouble(course -> course.percentMale)));
    Map<String, Double> mapToBach = courses.stream()
            .collect(Collectors.groupingBy(course -> course.number,
                     Collectors.averagingDouble(course -> course.percentDegree)));
    // number to value
    Map<String, Double> mapToAvg = mapToAge.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, course -> {
              String num = course.getKey();
              Double avgage = mapToAge.get(num);
              Double avggen = mapToGen.get(num);
              Double avgbach = mapToBach.get(num);
              double value = Math.pow((age - avgage), 2)
                      + Math.pow((gender * 100 - avggen), 2)
                      + Math.pow((isBachelorOrHigher * 100 - avgbach), 2);
              return value;
            }));
    // Find number~title
    Map<String, List<Course>> numberToCourse = courses.stream()
            .collect(Collectors.groupingBy(course -> course.number));
    Map<String, String> numberToTitle = new HashMap<>();
    for (Map.Entry<String, List<Course>> e : numberToCourse.entrySet()) {
      String newKey = e.getKey();
      List<Course> courses = e.getValue();
      Course latestCourse = Collections.max(courses,
                                            Comparator.comparing(course -> course.launchDate));
      numberToTitle.put(newKey, latestCourse.title);
    }
    Map<String, Double> titleToValue = new HashMap<>();
    for (Map.Entry<String, Double> e : mapToAvg.entrySet()) {
      String newKey = numberToTitle.get(e.getKey());
      Double newValue = e.getValue();
      titleToValue.put(newKey, newValue);
    }
    Comparator<Map.Entry<String, Double>> myComparator = (e1, e2) -> {
      if (e1.getValue().equals(e2.getValue())) {
        return e1.getKey().compareTo(e2.getKey());
      } else {
        return e1.getValue().compareTo(e2.getValue());
      }
    };

    List<String> result = titleToValue.entrySet().stream()
            .sorted(myComparator)
            .limit(10).map(e -> e.getKey()).toList();
    return result;
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