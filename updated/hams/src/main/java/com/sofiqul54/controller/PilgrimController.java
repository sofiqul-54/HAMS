package com.sofiqul54.controller;


import com.sofiqul54.entity.BookingSummary;
import com.sofiqul54.entity.GroupLeaderSummary;
import com.sofiqul54.entity.Groupleader;
import com.sofiqul54.entity.Pilgrim;
import com.sofiqul54.jasper.GroupleaderService;
import com.sofiqul54.jasper.MediaTypeUtils;
import com.sofiqul54.jasper.PilgrimService;
import com.sofiqul54.repo.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;



@Controller
@RequestMapping(value = "/pilgrim/")
public class PilgrimController {
    @Autowired
    private ImageOptimizer imageOptimizer;

    @Autowired
    private PilgrimRepo repo;

    @Autowired
    private PackageRepo packageRepo;

    @Autowired
    private GroupleaderRepo groupleaderRepo;
    @Autowired
    private BookingSummaryRepo bookingSummaryRepo;

    @Autowired
    private GroupleaderSummaryRepo groupleaderSummaryRepo;

    @Autowired
    private PilgrimService pilgrimService;

    @Autowired
    ServletContext context;




    @GetMapping(value = "add")
    public String viewAdd(Pilgrim pilgrim, Model model) {
        model.addAttribute("packagelist", this.packageRepo.findAll());
        model.addAttribute("grouplist", this.groupleaderRepo.findAll());
        return "pilgrims/add";
    }

    @PostMapping(value = "add")
    public String bookingSave(@Valid Pilgrim pilgrim, BindingResult bindingResult, Model model) {

        if (bindingResult.hasErrors()) {
            return "pilgrims/add";
        } else {
            if (pilgrim.getEmail() != null) {

                if (repo.existsByEmail(pilgrim.getEmail())) {
                    model.addAttribute("rejectMsg", "UserName allready exist");
                } else {
                    pilgrim.setRegiDate(new Date());
                    this.repo.save(pilgrim);
                        /*booking Summary*/
                    double due = pilgrim.getPpackage().getPrice() - pilgrim.getBookingAmount();
                    double aol = pilgrim.getBookingAmount();

                    BookingSummary bookingSummary = new BookingSummary(pilgrim.getPpackage().getPrice(), pilgrim.getBookingAmount(), due, pilgrim, pilgrim.getPpackage());
                    bookingSummaryRepo.save(bookingSummary);

                        /*groupleader Summary*/
                    GroupLeaderSummary summary=groupleaderSummaryRepo.findByGroupleader(groupleaderRepo.getOne(pilgrim.getGroupleader().getId()));

                    try{
                        double comm = pilgrim.getGroupleader().getCommission();
                        double com = pilgrim.getPpackage().getPrice() * (comm / 100);
                        double totalCom = summary.getTotalCommission() + com;

                        summary.setTotalCommission(totalCom);
                        groupleaderSummaryRepo.save(summary);
                    }catch(NullPointerException ne){
                        double comm = pilgrim.getGroupleader().getCommission();
                        double com = pilgrim.getPpackage().getPrice() * (comm / 100);


                        double totalCom =  com;

                        GroupLeaderSummary groupLeaderSummary = new GroupLeaderSummary(pilgrim.getGroupleader().getLeaderName(), com,
                                totalCom, pilgrim, groupleaderRepo.getOne(pilgrim.getGroupleader().getId()));
                        groupleaderSummaryRepo.save(groupLeaderSummary);
                    }


                    model.addAttribute("pilgrim", new Pilgrim());
                    model.addAttribute("successMsg", "Congratulations! Data save sucessfully");
                }
            }
        }
        model.addAttribute("packagelist", this.packageRepo.findAll());
        model.addAttribute("grouplist", this.groupleaderRepo.findAll());
        return "pilgrims/add";
    }


    @GetMapping(value = "edit/{id}")
    public String viewEdit(Model model, @PathVariable("id") Long id) {
        model.addAttribute("pilgrim", repo.getOne(id));
        model.addAttribute("packagelist", this.packageRepo.findAll());
        model.addAttribute("grouplist", this.groupleaderRepo.findAll());

        return "pilgrims/edit";
    }

    @PostMapping(value = "edit/{id}")
    public String edit(@Valid Pilgrim pilgrim, BindingResult bindingResult, Model model, @PathVariable("id") Long id) {
        if (bindingResult.hasErrors()) {
            return "pilgrims/edit";
        }
        Optional<Pilgrim> pil = this.repo.findByEmail(pilgrim.getEmail());
        if (pil.get().getId() != id) {
            model.addAttribute("rejectMsg", "Already Have This Entry");
            return "pilgrims/edit";
        } else {
            pilgrim.setRegiDate(new Date());
            pilgrim.setId(id);
            this.repo.save(pilgrim);
            model.addAttribute("packagelist", this.packageRepo.findAll());
            model.addAttribute("grouplist", this.groupleaderRepo.findAll());
        }
        return "redirect:/pilgrim/list";
    }

    @GetMapping(value = "del/{id}")
    public String del(@PathVariable("id") Long id) {
        if (id != null) {
            this.repo.deleteById(id);
        }
        return "redirect:/pilgrim/list";
    }

    @GetMapping(value = "list")
    public String list(Model model) {
        model.addAttribute("list", this.repo.findAll());
        return "pilgrims/list";
    }

    /*Pilgrim Summary*/
    @GetMapping(value = "summary")
    public String summaryView(Model model) {
        model.addAttribute("summarylist", this.bookingSummaryRepo.findAll());
        return "summary/summary";

    }

    /*=====================Jasper Report===============*/
    @RequestMapping(value = "pilgrimreport", method = RequestMethod.GET)
    public void report(HttpServletResponse response) throws Exception {
        response.setContentType("text/html");
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(pilgrimService.report());
        InputStream inputStream = this.getClass().getResourceAsStream("/pilgrim/report.jrxml");
        JasperReport jasperReport = JasperCompileManager.compileReport(inputStream);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, null, dataSource);
        HtmlExporter exporter = new HtmlExporter(DefaultJasperReportsContext.getInstance());
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleHtmlExporterOutput(response.getWriter()));
        exporter.exportReport();
    }

    /*@RequestMapping(value = "/pdf", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_PDF_VALUE)
    public void reportPdf(HttpServletResponse response) throws Exception {
        String source = "E:\\J2EE_ALL_(OWN)\\Git_Own\\hams\\hams\\src\\main\\resources\\groupleaderreport.jrxml";
        try {
                JasperCompileManager.compileReportToFile(source);
        } catch (JRException e) {
            e.printStackTrace();
        }
        String sourceFileName = "E:\\J2EE_ALL_(OWN)\\Git_Own\\hams\\hams\\src\\main\\resources\\groupleaderreport1.jasper";

        String printFileName = null;
        String destFileName = "E:\\J2EE_ALL_(OWN)\\Git_Own\\hams\\hams\\src\\main\\resources\\groupleaderreport.pdf";
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(groupleaderService.report());
        Map parameters = new HashMap();
        try {
            printFileName = JasperFillManager.fillReportToFile(sourceFileName,
                    parameters, dataSource);
            if (printFileName != null) {
                JasperExportManager.exportReportToPdfFile(printFileName,
                        destFileName);
            }
        } catch (JRException e) {
            e.printStackTrace();
        }
    }*/

    ////////////////pdf//////////////////////


    public void pilreportPdf() throws Exception {
        String source = "src\\main\\resources\\pilgrim\\report.jrxml";
        try {
            JasperCompileManager.compileReportToFile(source);
        } catch (JRException e) {
            e.printStackTrace();
        }
        String sourceFileName = "src\\main\\resources\\pilgrim\\report1.jasper";
        String printFileName = null;
        String destFileName = "src\\main\\resources\\pilgrim\\pilgrim\\report.pdf";
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(pilgrimService.report());
        Map parameters = new HashMap();
        try {
            printFileName = JasperFillManager.fillReportToFile(sourceFileName,
                    parameters, dataSource);
            if (printFileName != null) {
                JasperExportManager.exportReportToPdfFile(printFileName,
                        destFileName);
            }
        } catch (JRException e) {
            e.printStackTrace();
        }

    }

    @RequestMapping("pilgrimpdf")
    public ResponseEntity<InputStreamResource> downloadFile1() throws IOException {
        try {
            pilreportPdf();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String fileName="src\\main\\resources\\pilgrim\\report.pdf";
        MediaType mediaType = MediaTypeUtils.getMediaTypeForFileName(this.context, fileName);

        File file = new File(fileName);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                // Content-Disposition
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
                // Content-Type
                .contentType(mediaType)
                // Contet-Length
                .contentLength(file.length())
                .body(resource);
    }

}