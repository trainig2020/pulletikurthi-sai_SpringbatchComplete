package com.satya.springbatchexample.config;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.satya.springbatchexample.batch.DBWriter;
import com.satya.springbatchexample.batch.DBWriter1;
import com.satya.springbatchexample.batch.Processor;
import com.satya.springbatchexample.controller.LoadController;
import com.satya.springbatchexample.model.User;

@Configuration
@EnableBatchProcessing
public class SpringBatchConfig {

//	@Bean
//	public Job job(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory,
//			ItemReader<User> itemReader, ItemProcessor<User, User> itemProcessor, ItemWriter<User> itemWriter) {
//
//		Step step = stepBuilderFactory.get("step").<User, User>chunk(3).reader(itemReader)
//				.processor(itemProcessor).writer(itemWriter)
//				.taskExecutor(taskExecutor())
//				.build();
//
//		return jobBuilderFactory.get("job")
//				.incrementer(new RunIdIncrementer())
//				.start(step)
//				.build();
//	}

//	@Bean
//	public MultiResourceItemReader<User> itemReader() {
//
//		MultiResourceItemReader<User> multiFileItemReader = new MultiResourceItemReader<>();
//		List<FileSystemResource> fileSystemResources = new ArrayList<>();
//		try {
//			// Path dir= Paths.get("F:/csvfiles");
//			Stream<Path> stream = Files.list(Paths.get("F:\\csvfiles"));
//			stream.forEach(x -> {
//				fileSystemResources.add(new FileSystemResource(x.toFile()));
//			});
//			Resource[] resources = {};
//			resources = fileSystemResources.toArray(resources);
//			for (Resource resource : resources) {
//				System.out.println("file names :"+resource.getFilename());
//			}
//			multiFileItemReader.setResources(resources);
//			multiFileItemReader.setDelegate(reader());
//			// new MonitoringConfig(dir).processEvents();
//			multiFileItemReader.setStrict(Boolean.FALSE);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return multiFileItemReader;
//	}
//
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	@Bean
//	public FlatFileItemReader<User> reader() {
//		// Create reader instance
//		FlatFileItemReader<User> reader = new FlatFileItemReader<User>();
//
//		// Set number of lines to skips. Use it if file has header rows.
//		reader.setLinesToSkip(1);
//		reader.setStrict(false);
//
//		// Configure how each line will be parsed and mapped to different values
//		reader.setLineMapper(new DefaultLineMapper() {
//			{
//				// 5 columns in each row
//				setLineTokenizer(new DelimitedLineTokenizer() {
//					{
//						setNames(new String[] { "id", "name", "dept", "salary" });
//					}
//				});
//				// Set values in User class
//				setFieldSetMapper(new BeanWrapperFieldSetMapper<User>() {
//					{
//						setTargetType(User.class);
//					}
//				});
//			}
//		});
//		return reader;
//	}

	static Resource[] resources;

	public SpringBatchConfig(Resource[] resources) {
		super();
		this.resources = resources;
		System.out.println("res in constructor " + resources[0].getFilename());
		getResources();
	}

	public Resource[] getResources() {

		System.out.println("res in getter " + resources[0].getFilename());
		return resources;
	}

	public void setResources(Resource[] resources) {
		this.resources = resources;
	}

	public SpringBatchConfig() {
		super();
	}

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Autowired
	private DBWriter dbwriter;

	@Autowired
	private DBWriter1 dbwriter1;

	@Autowired
	private Processor process;

	// first job Auto
	@Bean
	// @Primary
	public Job job() {
		return jobBuilderFactory.get("job").incrementer(new RunIdIncrementer()).start(step()).build();
	}

	@Bean(name = "job1")
	public Job job1() {
		return jobBuilderFactory.get("job1").incrementer(new RunIdIncrementer()).start(step1()).build();

	}

	@Bean
	public Step step() {
		return stepBuilderFactory.get("step").<User, User>chunk((3))

				.reader(itemReader())

				.processor(process)

				.writer(dbwriter).taskExecutor(taskExecutor()).build();
	}

	@Bean
	public Step step1() {
		return stepBuilderFactory.get("step1").<User, User>chunk((3))

				.reader(itemReader1())

//				.processor(process)

				.writer(dbwriter1).build();
	}

	@Bean
	@Qualifier
	@StepScope
	public MultiResourceItemReader<User> itemReader() {
		MultiResourceItemReader<User> resourceItemReader = new MultiResourceItemReader<User>();

		ClassLoader cl = this.getClass().getClassLoader();
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
		// Resource[] res = null;

		try {
			Resource[] resources = resolver.getResources("file:C:/Users/sivan/Document/MyCsv/person*.csv");
			for (Resource resource : resources) {
				System.out.println("file names :" + resource.getFilename());
				// lisSrc.add(resource);
			}
			resourceItemReader.setResources(resources);
			resourceItemReader.setDelegate(reader());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("new file " + resourceItemReader.getCurrentResource());

		return resourceItemReader;
	}

	@Bean
	@Primary
	@StepScope
	public FlatFileItemReader<User> reader() {
		// Create reader instance
		FlatFileItemReader<User> reader = new FlatFileItemReader<User>();
		// Set number of lines to skips. Use it if file has header rows.
		System.out.println("file names " + reader.toString());
		reader.setLinesToSkip(1);
		System.out.println(reader.toString());
		// Configure how each line will be parsed and mapped to different values
		reader.setLineMapper(new DefaultLineMapper<User>() {
			{
				// 3 columns in each row
				setLineTokenizer(new DelimitedLineTokenizer() {
					{
						setNames(new String[] { "id", "name", "dept", "salary" });
					}
				});
				// Set values in User class
				setFieldSetMapper(new BeanWrapperFieldSetMapper<User>() {
					{
						setTargetType(User.class);
					}
				});
			}
		});
		return reader;
	}

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setMaxPoolSize(10);
		taskExecutor.afterPropertiesSet();
		taskExecutor.getActiveCount();

		return taskExecutor;
	}

	// second job : Manual

	@Bean
	@StepScope
	@Qualifier
	public MultiResourceItemReader<User> itemReader1() {

		// if(LoadController.getRes() !=null) {
		// Resource[] ref = LoadController.getRes().;
		MultiResourceItemReader<User> resourceItemReader3 = new MultiResourceItemReader<User>();
		/*
		 * LoadController lcv = new LoadController(); Resource[] rdf = lcv.getRes();
		 */
		SpringBatchConfig spd = new SpringBatchConfig();
		resources = spd.getResources();
		for (Resource resource : resources) {
			System.out.println("resource name " + resource.getFilename());
		}
		System.out.println("executes multi item reader");
		resourceItemReader3.setResources(resources);
		resourceItemReader3.setDelegate(reader1());

		return resourceItemReader3;

	}

	@Bean
	@Qualifier
	@StepScope
	public FlatFileItemReader<User> reader1() {
		// Create reader instance
		FlatFileItemReader<User> reader = new FlatFileItemReader<User>();
		// Set number of lines to skips. Use it if file has header rows.
		System.out.println("file names " + reader.toString());
		reader.setLinesToSkip(1);
		System.out.println(reader.toString());
		// Configure how each line will be parsed and mapped to different values
		reader.setLineMapper(new DefaultLineMapper<User>() {
			{
				// 3 columns in each row
				setLineTokenizer(new DelimitedLineTokenizer() {
					{
						setNames(new String[] { "id", "name", "dept", "salary" });
					}
				});
				// Set values in User class
				setFieldSetMapper(new BeanWrapperFieldSetMapper<User>() {
					{
						setTargetType(User.class);
					}
				});
			}
		});
		return reader;
	}

	@Bean
	public TaskExecutor taskExecutor1() {
		ThreadPoolTaskExecutor taskExecutor1 = new ThreadPoolTaskExecutor();
		taskExecutor1.setMaxPoolSize(10);
		taskExecutor1.afterPropertiesSet();
		taskExecutor1.getActiveCount();

		return taskExecutor1;
	}

//	@Bean
//	public ThreadPoolTaskExecutor taskExecutor() {
//		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
//		taskExecutor.setMaxPoolSize(10);
//		taskExecutor.setCorePoolSize(10);
//		taskExecutor.setQueueCapacity(10);
//		taskExecutor.afterPropertiesSet();
//		return taskExecutor;
//	}
//	
//	@Bean
//    public TaskExecutor taskExecutor() {
//        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor("spring_batch");
//        asyncTaskExecutor.setConcurrencyLimit(5);
//        return asyncTaskExecutor;
//    }

//  @Bean
//  @StepScope
//  public CSVReader csvReader()
//  {
//      return new CSVReader();
//  }

//  @Bean
//  public FlatFileItemReader<User> itemReader() {
//
//      FlatFileItemReader<User> flatFileItemReader = new FlatFileItemReader<>();
//      flatFileItemReader.setResource(new FileSystemResource("src/main/resources/users.csv"));
//      flatFileItemReader.setName("CSV-Reader");
//      flatFileItemReader.setLinesToSkip(1);
//      flatFileItemReader.setLineMapper(lineMapper());
//      return flatFileItemReader;
//  }

//    @Bean
//    public LineMapper<User> lineMapper() {
//
//        DefaultLineMapper<User> defaultLineMapper = new DefaultLineMapper<>();
//        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
//
//        lineTokenizer.setDelimiter(",");
//        lineTokenizer.setStrict(false);
//        lineTokenizer.setNames(new String[]{"id", "name", "dept", "salary"});
//
//        BeanWrapperFieldSetMapper<User> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
//        fieldSetMapper.setTargetType(User.class);
//
//        defaultLineMapper.setLineTokenizer(lineTokenizer);
//        defaultLineMapper.setFieldSetMapper(fieldSetMapper);
//
//        return defaultLineMapper;
//    }

}
