package com.sofiqul54.controller;

import com.sofiqul54.entity.Role;
import com.sofiqul54.repo.AgencyRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import java.util.Optional;

@Controller
@RequestMapping(value = "/agninfo/")
public class AgencyController {

    @Autowired
    private AgencyRepo agencyRepo;

    @GetMapping(value = "add")
    public String viewAdd(Model model) {
        model.addAttribute("role", new Role());
        return "roles/add";
    }

    @PostMapping(value = "add")
    public String add(@Valid Role role, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "roles/add";
        }
        if (agencyRepo.existsRoleByRoleName(role.getRoleName())) {
            model.addAttribute("rejectMsg", "Already Have This Entry");
        } else {
            role.setRoleName(role.getRoleName().toUpperCase());
            this.agencyRepo.save(role);
            model.addAttribute("successMsg", "Successfully Saved!");
        }
        return "roles/add";
    }

    @GetMapping(value = "edit/{id}")
    public String viewEdit(Model model, @PathVariable("id") Long id) {
        model.addAttribute("role", agencyRepo.getOne(id));
        return "roles/edit";
    }

    @PostMapping(value = "edit/{id}")
    public String edit(@Valid Role role, BindingResult bindingResult, Model model, @PathVariable("id") Long id) {
        if (bindingResult.hasErrors()) {
            return "roles/edit";
        }
        Optional<Role> rol = this.agencyRepo.findByRoleName(role.getRoleName());
        if (rol.get().getId() != id) {
            model.addAttribute("rejectMsg", "Already Have This Entry");
            return "roles/edit";
        } else {
            role.setId(id);
            role.setRoleName(role.getRoleName().toUpperCase());
            this.agencyRepo.save(role);
        }
        return "redirect:/role/list";
    }

    @GetMapping(value = "del/{id}")
    public String del(@PathVariable("id") Long id) {
        if (id != null) {
            this.agencyRepo.deleteById(id);
        }
        return "redirect:/role/list";
    }

    @GetMapping(value = "list")
    public String list(Model model){
        model.addAttribute("list", this.agencyRepo.findAll());
    return "rolelist";
    }
}