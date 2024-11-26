package com.example.schoolapp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private List<String> cities;
    private Map<String, List<String>> classesPerCity;
    private Map<String, List<String>> subjectsPerClass;
    private Map<String, Map<String, List<Integer>>> gradesPerStudent;
    private Map<String, Map<String, Boolean>> attendancePerStudent;
    private Stack<Runnable> navigationStack = new Stack<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.cityRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        initializeData();
        setupCityList();
    }

    private void initializeData() {
        cities = Arrays.asList("New York", "Los Angeles", "Chicago");

        classesPerCity = new HashMap<>();
        classesPerCity.put("Warszawa", Arrays.asList("Klasa 1", "Klasa 2", "Klasa 3", "Klasa 4", "Klasa 5", "Klasa 6", "Klasa 7", "Klasa 8"));
        classesPerCity.put("Kraków", Arrays.asList("Klasa 1", "Klasa 2", "Klasa 3", "Klasa 4", "Klasa 5", "Klasa 6", "Klasa 7", "Klasa 8"));
        classesPerCity.put("Rzeszów", Arrays.asList("Klasa 1", "Klasa 2", "Klasa 3", "Klasa 4", "Klasa 5", "Klasa 6", "Klasa 7", "Klasa 8"));

        List<String> subjects = Arrays.asList("Matematyka", "Fizyka", "Język Angielski", "Historia", "Geografia", "Plastyka", "Muzyka", "WF", "Informatyka", "Biologia", "Chemia", "Jezyk niemiecki");

        subjectsPerClass = new HashMap<>();
        for (String city : cities) {
            for (String className : classesPerCity.get(city)) {
                List<String> shuffledSubjects = new ArrayList<>(subjects);
                Collections.shuffle(shuffledSubjects);
                subjectsPerClass.put(className, shuffledSubjects);
            }
        }

        gradesPerStudent = new HashMap<>();
        attendancePerStudent = new HashMap<>();

        List<String> firstNames = Arrays.asList("Michał", "Paweł", "Karol", "Kacper", "Rysiu", "Radek", "Artur", "Maksymilian", "Krystian", "Adrian");
        List<String> lastNames = Arrays.asList("Urban", "Rzeszutek", "Łojszczyk", "Bucki", "Baran", "Połeć", "Kępa", "Pociask", "Kalita", "Krupa");

        Random random = new Random();

        for (String city : cities) {
            for (String className : classesPerCity.get(city)) {
                for (int i = 1; i <= 30; i++) {
                    String studentName = firstNames.get(random.nextInt(firstNames.size())) + " " + lastNames.get(random.nextInt(lastNames.size()));
                    Map<String, List<Integer>> studentGrades = new HashMap<>();
                    Map<String, Boolean> studentAttendance = new HashMap<>();
                    for (String subject : subjectsPerClass.get(className)) {
                        List<Integer> grades = new ArrayList<>();
                        for (int j = 0; j < 4; j++) {
                            grades.add(random.nextInt(5) + 1);  // Grades from 1 to 5
                        }
                        studentGrades.put(subject, grades);
                        studentAttendance.put(subject, random.nextDouble() > 0.1);
                    }
                    gradesPerStudent.put(studentName, studentGrades);
                    attendancePerStudent.put(studentName, studentAttendance);
                }
            }
        }
    }

    private void setupCityList() {
        navigationStack.clear();
        CityAdapter adapter = new CityAdapter(cities, this::showClassList);
        recyclerView.setAdapter(adapter);
    }

    private void showClassList(String city) {
        setContentView(R.layout.activity_class_list);
        TextView cityNameTextView = findViewById(R.id.cityNameTextView);
        cityNameTextView.setText(city);
        RecyclerView classRecyclerView = findViewById(R.id.classRecyclerView);
        classRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ClassAdapter classAdapter = new ClassAdapter(classesPerCity.get(city), className -> showTimetable(city, className));
        classRecyclerView.setAdapter(classAdapter);

        navigationStack.push(this::setupCityList);
        setupBackButton();
    }

    private void showTimetable(String city, String className) {
        setContentView(R.layout.activity_timetable);
        TextView classNameTextView = findViewById(R.id.classNameTextView);
        classNameTextView.setText(className);
        RecyclerView timetableRecyclerView = findViewById(R.id.timetableRecyclerView);
        timetableRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        SubjectAdapter subjectAdapter = new SubjectAdapter(subjectsPerClass.get(className),
                new SubjectAdapter.OnSubjectClickListener() {
                    @Override
                    public void onViewGrades(String subject) {
                        showStudentList(city, className, subject);
                    }

                    @Override
                    public void onViewAttendance(String subject) {
                        showAttendanceList(city, className, subject);
                    }
                });
        timetableRecyclerView.setAdapter(subjectAdapter);

        navigationStack.push(() -> showClassList(city));
        setupBackButton();
    }

    private void showStudentList(String city, String className, String subject) {
        setContentView(R.layout.activity_student_list);
        TextView subjectNameTextView = findViewById(R.id.subjectNameTextView);
        subjectNameTextView.setText(subject);
        RecyclerView studentRecyclerView = findViewById(R.id.studentRecyclerView);
        studentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<String> students = new ArrayList<>(gradesPerStudent.keySet());
        StudentAdapter studentAdapter = new StudentAdapter(students, gradesPerStudent, subject);
        studentRecyclerView.setAdapter(studentAdapter);

        findViewById(R.id.downloadGradesButton).setOnClickListener(v -> downloadGrades(city, className, subject));

        navigationStack.push(() -> showTimetable(city, className));
        setupBackButton();
    }

    private void showAttendanceList(String city, String className, String subject) {
        setContentView(R.layout.attendance_list);
        TextView subjectNameTextView = findViewById(R.id.subjectNameTextView);
        subjectNameTextView.setText(subject);
        RecyclerView attendanceRecyclerView = findViewById(R.id.attendanceRecyclerView);
        attendanceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<String> students = new ArrayList<>(attendancePerStudent.keySet());
        AttendanceAdapter attendanceAdapter = new AttendanceAdapter(students, attendancePerStudent, subject);
        attendanceRecyclerView.setAdapter(attendanceAdapter);

        findViewById(R.id.downloadAttendanceButton).setOnClickListener(v -> downloadAttendance(city, className, subject));

        navigationStack.push(() -> showTimetable(city, className));
        setupBackButton();
    }

    private void setupBackButton() {
        findViewById(R.id.backButton).setOnClickListener(v -> {
            if (!navigationStack.isEmpty()) {
                navigationStack.pop().run();
            }
        });
    }

    private void downloadGrades(String city, String className, String subject) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);

        canvas.drawText("Grades for " + city + " - " + className + " - " + subject, 50, 50, paint);

        int y = 100;
        for (Map.Entry<String, Map<String, List<Integer>>> entry : gradesPerStudent.entrySet()) {
            String studentName = entry.getKey();
            List<Integer> grades = entry.getValue().get(subject);
            String gradeText = studentName + ": " + grades.toString();
            canvas.drawText(gradeText, 50, y, paint);
            y += 20;
        }

        document.finishPage(page);

        String fileName = city + "_" + className + "_" + subject + "_grades.pdf";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);

        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "Grades downloaded: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error downloading grades", Toast.LENGTH_SHORT).show();
        }

        document.close();
    }

    private void downloadAttendance(String city, String className, String subject) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);

        canvas.drawText("Attendance for " + city + " - " + className + " - " + subject, 50, 50, paint);

        int y = 100;
        for (Map.Entry<String, Map<String, Boolean>> entry : attendancePerStudent.entrySet()) {
            String studentName = entry.getKey();
            String attendanceText = studentName + ": " + (int)(Math.random() * (100 - 50 + 1)) + 50; ;
            canvas.drawText(attendanceText, 50, y, paint);
            y += 20;
        }

        document.finishPage(page);

        String fileName = city + "_" + className + "_" + subject + "_attendance.pdf";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);

        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "Attendance downloaded: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error downloading attendance", Toast.LENGTH_SHORT).show();
        }

        document.close();
    }

    private class CityAdapter extends RecyclerView.Adapter<CityAdapter.CityViewHolder> {
        private List<String> cities;
        private OnItemClickListener listener;

        public CityAdapter(List<String> cities, OnItemClickListener listener) {
            this.cities = cities;
            this.listener = listener;
        }

        @NonNull
        @Override
        public CityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_city_item, parent, false);
            return new CityViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CityViewHolder holder, int position) {
            holder.bind(cities.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return cities.size();
        }

        class CityViewHolder extends RecyclerView.ViewHolder {
            private TextView cityNameTextView;

            public CityViewHolder(@NonNull View itemView) {
                super(itemView);
                cityNameTextView = itemView.findViewById(R.id.cityNameTextView);
            }

            public void bind(final String city, final OnItemClickListener listener) {
                cityNameTextView.setText(city);
                itemView.setOnClickListener(v -> listener.onItemClick(city));
            }
        }
    }

    private class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {
        private List<String> classes;
        private OnItemClickListener listener;

        public ClassAdapter(List<String> classes, OnItemClickListener listener) {
            this.classes = classes;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_class_item, parent, false);
            return new ClassViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
            holder.bind(classes.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return classes.size();
        }

        class ClassViewHolder extends RecyclerView.ViewHolder {
            private TextView classNameTextView;

            public ClassViewHolder(@NonNull View itemView) {
                super(itemView);
                classNameTextView = itemView.findViewById(R.id.classNameTextView);
            }

            public void bind(final String className, final OnItemClickListener listener) {
                classNameTextView.setText(className);
                itemView.setOnClickListener(v -> listener.onItemClick(className));
            }
        }
    }

    private static class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder> {
        private List<String> subjects;
        private OnSubjectClickListener listener;

        public interface OnSubjectClickListener {
            void onViewGrades(String subject);
            void onViewAttendance(String subject);
        }

        public SubjectAdapter(List<String> subjects, OnSubjectClickListener listener) {
            this.subjects = subjects;
            this.listener = listener;
        }

        @NonNull
        @Override
        public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subject_item, parent, false);
            return new SubjectViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
            holder.bind(subjects.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return subjects.size();
        }

        class SubjectViewHolder extends RecyclerView.ViewHolder {
            private TextView subjectNameTextView;
            private Button viewGradesButton;
            private Button viewAttendanceButton;

            public SubjectViewHolder(@
                                             NonNull View itemView) {
                super(itemView);
                subjectNameTextView = itemView.findViewById(R.id.subjectNameTextView);
                viewGradesButton = itemView.findViewById(R.id.viewGradesButton);
                viewAttendanceButton = itemView.findViewById(R.id.viewAttendanceButton);
            }

            public void bind(final String subject, final OnSubjectClickListener listener) {
                subjectNameTextView.setText(subject);
                viewGradesButton.setOnClickListener(v -> listener.onViewGrades(subject));
                viewAttendanceButton.setOnClickListener(v -> listener.onViewAttendance(subject));
            }
        }
    }

    private class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
        private List<String> students;
        private Map<String, Map<String, List<Integer>>> gradesPerStudent;
        private String subject;

        public StudentAdapter(List<String> students, Map<String, Map<String, List<Integer>>> gradesPerStudent, String subject) {
            this.students = students;
            this.gradesPerStudent = gradesPerStudent;
            this.subject = subject;
        }

        @NonNull
        @Override
        public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_student_item, parent, false);
            return new StudentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
            String student = students.get(position);
            List<Integer> grades = gradesPerStudent.get(student).get(subject);
            holder.bind(student, grades);
        }

        @Override
        public int getItemCount() {
            return students.size();
        }

        class StudentViewHolder extends RecyclerView.ViewHolder {
            private TextView studentNameTextView;
            private TextView studentGradesTextView;

            public StudentViewHolder(@NonNull View itemView) {
                super(itemView);
                studentNameTextView = itemView.findViewById(R.id.studentNameTextView);
                studentGradesTextView = itemView.findViewById(R.id.studentGradesTextView);
            }

            public void bind(String student, List<Integer> grades) {
                studentNameTextView.setText(student);
                studentGradesTextView.setText(grades.toString().replaceAll("[\\[\\]]", ""));
            }
        }
    }

    private class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {
        private List<String> students;
        private Map<String, Map<String, Boolean>> attendancePerStudent;
        private String subject;

        public AttendanceAdapter(List<String> students, Map<String, Map<String, Boolean>> attendancePerStudent, String subject) {
            this.students = students;
            this.attendancePerStudent = attendancePerStudent;
            this.subject = subject;
        }

        @NonNull
        @Override
        public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new AttendanceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
            String student = students.get(position);
            boolean isPresent = attendancePerStudent.get(student).get(subject);
            holder.bind(student, isPresent);
        }

        @Override
        public int getItemCount() {
            return students.size();
        }

        class AttendanceViewHolder extends RecyclerView.ViewHolder {
            private TextView text1;
            private TextView text2;

            public AttendanceViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }

            public void bind(String student, boolean isPresent) {
                text1.setText(student);
                text2.setText(isPresent ? "Present" : "Absent");
            }
        }
    }

    private interface OnItemClickListener {
        void onItemClick(String item);
    }

    @Override
    public void onBackPressed() {
        if (!navigationStack.isEmpty()) {
            navigationStack.pop().run();
        } else {
            super.onBackPressed();
        }
    }
}

