package ru.kata.spring.boot_security.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import ru.kata.spring.boot_security.demo.configs.dto.UserDTO;
import ru.kata.spring.boot_security.demo.configs.dto.UserMapper;
import ru.kata.spring.boot_security.demo.configs.dto.UserUpdateRequestDTO;
import ru.kata.spring.boot_security.demo.entities.Role;
import ru.kata.spring.boot_security.demo.entities.User;
import ru.kata.spring.boot_security.demo.service.AdminService;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.UserService;
import ru.kata.spring.boot_security.demo.util.UserErrorResponse;
import ru.kata.spring.boot_security.demo.util.UserNotCreatedException;
import ru.kata.spring.boot_security.demo.util.UserNotFoundException;

import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminsController {

    private final AdminService adminService;
    private final RoleService roleService;
    private final UserMapper userMapper;

    @Autowired
    public AdminsController(AdminService adminService, RoleService roleService, UserMapper userMapper) {
        this.roleService = roleService;
        this.adminService = adminService;
        this.userMapper = userMapper;
    }

    @GetMapping()
    public List<User> allUsers() {
        return adminService.findAll();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable("id") int id) {
        return adminService.findById(id);
    }


    @PostMapping
    public ResponseEntity<HttpStatus> saveUser(@RequestBody @Valid UserDTO userDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            StringBuilder errors = new StringBuilder();
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                errors.append(fieldError.getField())
                        .append(" - ").append(fieldError.getDefaultMessage())
                        .append(";")
                        .append("\n");
            }
            throw new UserNotCreatedException(errors.toString());
        }
        User user = userMapper.toEntity(userDTO);
        adminService.save(user);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") int id, @RequestBody UserUpdateRequestDTO userUpdateRequestDTO) {
        User existingUser = adminService.findById(id);
        if (existingUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        existingUser.setUsername(userUpdateRequestDTO.getUsername());
        existingUser.setPassword(userUpdateRequestDTO.getPassword());
        existingUser.setFirstName(userUpdateRequestDTO.getFirstName());
        existingUser.setLastName(userUpdateRequestDTO.getLastName());
        existingUser.setAge(userUpdateRequestDTO.getAge());

        Set<Role> roles = new HashSet<>();
        for (String roleName : userUpdateRequestDTO.getRoles()) {
            Role role = roleService.findByName(roleName);
            if (role != null) {
                roles.add(role);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid role");
            }
        }
        existingUser.setRoles(roles);
        adminService.updateById(id, existingUser);
        return ResponseEntity.ok(HttpStatus.OK);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") int id) {
        adminService.deleteById(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @ExceptionHandler(UserNotFoundException.class)
    private ResponseEntity<UserErrorResponse> handleException(UserNotFoundException exception) {
        UserErrorResponse response = new UserErrorResponse(
                "User with this id wasn't found",
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserNotCreatedException.class)
    private ResponseEntity<UserErrorResponse> handleException(UserNotCreatedException exception) {
        UserErrorResponse response = new UserErrorResponse(
                exception.getMessage(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

}
