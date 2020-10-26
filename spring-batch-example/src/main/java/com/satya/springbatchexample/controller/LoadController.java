package com.satya.springbatchexample.controller;

import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import com.satya.springbatchexample.config.Monitoring;
import com.satya.springbatchexample.config.SpringBatchConfig;
import com.sun.el.parser.ParseException;

import javaxt.io.Directory;
import javaxt.io.Directory.Event;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/load")
public class LoadController {

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	Job job;

	@Autowired
	@Qualifier("job1")
	Job job1;

	static Resource[] res;

	@GetMapping
	public BatchStatus load() throws JobParametersInvalidException, JobExecutionAlreadyRunningException,
			JobRestartException, JobInstanceAlreadyCompleteException, IOException, InterruptedException {
		LoadController lc = new LoadController();
		Map<String, JobParameter> maps = new HashMap<>();
		maps.put("time", new JobParameter(System.currentTimeMillis()));
		JobParameters parameters = new JobParameters(maps);
		JobExecution jobExecution = jobLauncher.run(job, parameters);
		System.out.println("JobExecution: " + jobExecution.getStatus());
		// lc.fileWatcherService();
		Directory folder = new Directory("C:\\Users\\sivan\\Document\\MyCsv");
		Directory folderCopy = new Directory("C:\\tempcopy");
		try {
			sync(folder, folderCopy);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (jobExecution.isRunning()) {
			System.out.println("...");
		}

		return jobExecution.getStatus();
	}

//	public void fileWatcherService() throws IOException, InterruptedException, JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
//        LoadController lc = new LoadController();
//        try (WatchService service = FileSystems.getDefault().newWatchService()) {
//            Map<WatchKey, Path> keyMap = new HashMap<>();
//            Path dir = Paths.get("F:\\csvfiles");
//            keyMap.put(dir.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
//                    StandardWatchEventKinds.ENTRY_MODIFY), dir);
//            WatchKey watchkey;
//            do {
//                watchkey = service.take();
//                Path eventdir = keyMap.get(watchkey);
//                for (WatchEvent<?> event : watchkey.pollEvents()) {
//                    WatchEvent.Kind<?> kind = event.kind();
//                    Path eventpath = (Path) event.context();
//                    System.out.println(eventdir + " : " + kind + " : " + eventpath);
//                    System.out.println(event.kind().toString());
//                    if (event.kind().toString().equalsIgnoreCase("ENTRY_CREATE")) {
//                    	Map<String, JobParameter> maps = new HashMap<>();
//                        maps.put("time4", new JobParameter(System.currentTimeMillis()));
//                        JobParameters parameters = new JobParameters(maps);
//						JobExecution jobExecution = jobLauncher.run(job, parameters);
//                        System.out.println("JobExecution: " + jobExecution.getStatus());
//                    }
//                }
//            } while (watchkey.reset());
//        }
//    }

	@SuppressWarnings("rawtypes")
	private void sync(Directory source, Directory destination) throws Exception {

		LoadController ld = new LoadController();
		List events = source.getEvents();
		while (true) {
			Event event;
			// Wait for new events to be added to the que
			synchronized (events) {
				while (events.isEmpty()) {
					try {
						System.out.println("waiting to do a event");
						events.wait();
						System.out.println("events are waiting");
					} catch (InterruptedException e) {
					}
				}
				event = (Event) events.remove(0);
			}
			int eventID = event.getEventID();
			System.out.println("EventId : " + eventID);
			if (eventID == Event.DELETE) {
				// Build path to the file in the destination directory
				String path = destination + "\\" + event.getFile().substring(source.toString().length());
				System.out.println("path is " + path);
				// Delete the file/directory
				new java.io.File(path).delete();
			} else {
				// Check if the event is associated with a file or directory so
				// we can use the JavaXT classes to create or modify the target.
				java.io.File obj = new java.io.File(event.getFile());
				if (obj.isDirectory()) {
					javaxt.io.Directory dir = new javaxt.io.Directory(obj);
					javaxt.io.Directory dest = new javaxt.io.Directory(
							destination + dir.toString().substring(source.toString().length()));
					switch (eventID) {
					case (Event.CREATE):
						dir.copyTo(dest, true);
						System.out.println("event creation");
						break;
					case (Event.MODIFY):
						System.out.println("event modification");
						break; // TODO
					case (Event.RENAME): {
						javaxt.io.Directory orgDir = new javaxt.io.Directory(event.getOriginalFile());
						dest = new javaxt.io.Directory(
								destination + orgDir.toString().substring(source.toString().length()));
						dest.rename(dir.getName());
						System.out.println("renaming");
						break;
					}
					}
				} else {
					javaxt.io.File file = new javaxt.io.File(obj);
					javaxt.io.File dest = new javaxt.io.File(
							destination + file.toString().substring(source.toString().length()));

					switch (eventID) {
					case (Event.CREATE):
						event.getFile();
						System.out.println("file name is " + event.getFile());
						LoadController lde = new LoadController();
						Map<String, JobParameter> maps = new HashMap<>();
						maps.put("time2", new JobParameter(System.currentTimeMillis()));
						JobParameters parameters = new JobParameters(maps);
						JobExecution jobExecution = jobLauncher.run(job, parameters);
						System.out.println("JobExecution: " + jobExecution.getStatus().toString());
						System.out.println("Batch is executed succesfully..");
						SpringBatchConfig spc = new SpringBatchConfig();
						ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) spc.taskExecutor();
						System.out.println(
								"value job " + job.getJobParametersIncrementer() + " " + taskExecutor.getActiveCount());
						System.out.println("else part created");
						break;
					case (Event.MODIFY):
						file.copyTo(dest, true);
						break;
					case (Event.RENAME): {
						javaxt.io.File orgFile = new javaxt.io.File(event.getOriginalFile());
						dest = new javaxt.io.File(
								destination + orgFile.toString().substring(source.toString().length()));
						dest.rename(file.getName());
						System.out.println("Renamed else part");
						break;

					}

					}
				}

			}

		}

	}

	@RequestMapping("/fileupload")
	public ModelAndView csvFileUpload(HttpServletRequest request,
			@RequestParam("fileUpload") MultipartFile[] fileUpload) throws Exception {
		File folderCopy = new File("C:\\Users\\sivan\\Document\\MyCsv");
		Path pathCopy = folderCopy.toPath();
		if (fileUpload != null && fileUpload.length > 0) {

			for (MultipartFile multipartFile : fileUpload) {

				System.out.println("Saving file: " + multipartFile.getOriginalFilename());
				System.out.println("Content of file :" + multipartFile.toString());

				try {
					byte[] bytes = multipartFile.getBytes();
					Path path = Paths.get(pathCopy + "\\" + multipartFile.getOriginalFilename());
					Files.write(path, bytes);
				} catch (IOException e) {

					e.printStackTrace();
				}

			}
		}
		Map<String, JobParameter> maps = new HashMap<>();
		maps.put("time5", new JobParameter(System.currentTimeMillis()));
		JobParameters parameters = new JobParameters(maps);
		JobExecution jobExecution = jobLauncher.run(job, parameters);
		System.out.println("parameters are " + parameters.toString());
		System.out.println("JobExecution: " + jobExecution.getStatus().toString());

		return new ModelAndView("entry");

	}

	@RequestMapping("/Fileslist")
	public ModelAndView manualSchedule() {
		File files = new File("C:\\Users\\sivan\\Document\\MyCsv");
		File[] listOfFiles = files.listFiles();
		List<String> Names = new ArrayList<>();
		ModelAndView model = new ModelAndView("entry");
		for (File file : listOfFiles) {

			Names.add(file.getName());
		}
		model.addObject("ListOfFiles", Names);

		return model;

	}

	@RequestMapping("/manualmode")
	public ModelAndView manualmodeSch(HttpServletRequest request, HttpServletResponse response) {

		Random rannum = new Random();

		String dateTimeLocal = request.getParameter("selecteddate");

		String[] fileNames = request.getParameterValues("names");

		ClassLoader cl = this.getClass().getClassLoader();
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
		// Resource[] res = null;
		ModelAndView mdv = new ModelAndView("entry");
		res = new Resource[fileNames.length];

		int i = 0;
		try {
			Resource[] resources = resolver.getResources("file:C:/Users/sivan/Document/MyCsv/person*.csv");

			for (Resource resource : resources) {

				for (String resource2 : fileNames) {

					if (resource.getFilename().equalsIgnoreCase(resource2)) {
						res[i] = resource;
						i++;
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();

		}

		for (Resource resource : res) {
			System.out.println("resorces selected " + resource.getFilename());

			System.out.println("Saving file: " + resource.getFilename());

		}
		TimerTask task = new TimerTask() {

			@Override
			public void run() {

				Map<String, JobParameter> maps = new HashMap<>();
				maps.put("time9", new JobParameter(System.currentTimeMillis()));
				// maps.put("reso", res);
				SpringBatchConfig mtb = new SpringBatchConfig(res);
				System.out.println("exceutes");
				JobParameters parameters = new JobParameters(maps);
				try {
					JobExecution jobExecution = jobLauncher.run(job1, parameters);
				} catch (JobExecutionAlreadyRunningException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JobRestartException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JobInstanceAlreadyCompleteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JobParametersInvalidException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		};

		try {
			Date futureDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(dateTimeLocal);
			System.out.println(futureDate);
			Timer timer = new Timer();
			timer.schedule(task, futureDate);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return mdv;

	}

}