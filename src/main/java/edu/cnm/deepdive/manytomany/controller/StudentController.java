package edu.cnm.deepdive.manytomany.controller;

import edu.cnm.deepdive.manytomany.model.dao.ProjectRepository;
import edu.cnm.deepdive.manytomany.model.dao.StudentRepository;
import edu.cnm.deepdive.manytomany.model.entity.Project;
import edu.cnm.deepdive.manytomany.model.entity.Student;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ExposesResourceFor(Student.class)
@RequestMapping("/students")
public class StudentController {

  private StudentRepository studentRepository;
  private ProjectRepository projectRepository;
  private EntityLinks entityLinks;

  @Autowired
  public StudentController(StudentRepository studentRepository,
      ProjectRepository projectRepository, EntityLinks entityLinks) {
    this.studentRepository = studentRepository;
    this.projectRepository = projectRepository;
    this.entityLinks = entityLinks;
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public List<Student> list() {
    return studentRepository.findAllByOrderByNameAsc();
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Student> post(@RequestBody Student student) {
    studentRepository.save(student);
    return ResponseEntity.created(student.getHref()).body(student);
  }

  @GetMapping(value = "{studentId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Student get(@PathVariable("studentId") long studentId) {
    return studentRepository.findById(studentId).get();
  }

  @GetMapping(value = "{studentId}/projects", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<Project> listProjects(@PathVariable("studentId") long studentId) {
    return get(studentId).getProjects();
  }

  @DeleteMapping(value = "{studentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("studentId") long studentId) {
    Student student = get(studentId);
    studentRepository.delete(student);
  }

  @PostMapping(value = "{studentId}/projects", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Project> postProject(@PathVariable("studentId") long studentId,
      @RequestBody Project partialProject) {
    Project project = projectRepository.findById(partialProject.getId()).get();
    Student student = get(studentId);
    student.getProjects().add(project);
    studentRepository.save(student);
    return ResponseEntity.created(
        entityLinks.linkForSingleResource(Student.class, studentId)
            .slash("projects")
            .slash(project.getId())
            .toUri()
    ).body(project);
  }

  @GetMapping(value = "{studentId}/projects/{projectId}")
  public Project getProject(@PathVariable("studentId") long studentId,
      @PathVariable("projectId") long projectId) {
    Student student = get(studentId);
    Project project = projectRepository.findById(projectId).get(); // Load entire object from DB.
    if (student.getProjects().contains(project)) {
      return project;
    } else {
      throw new NoSuchElementException();
    }
  }

  @DeleteMapping(value = "{studentId}/projects/{projectId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteProject(@PathVariable("studentId") long studentId,
      @PathVariable("projectId") long projectId) {
    Student student = get(studentId);
    Project project = getProject(studentId, projectId);
    student.getProjects().remove(project);
    studentRepository.save(student);
  }

  @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Resource not found")
  @ExceptionHandler(NoSuchElementException.class)
  public void notFound() {
  }

}
