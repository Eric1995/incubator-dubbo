package org.apache.dubbo.flux;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;

public class FluxStatFileExporter implements FluxStatExporter {

	private static final Logger logger = LoggerFactory.getLogger(FluxStatFileExporter.class);
	
	public static final String CSV_SUFFIX = ".csv";
	
	public static final String TITLE = "Time,Success,Failure,Timeout,Rejection,Overload,AvgElapse,MaxElapse,MaxConcurrent";
	
	public static final String ITEM_SEPERATOR = ",";
	
	public static final String LINE_SEPERATOR = System.getProperty("line.separator", "\n");
	
	public static final String DATE_REGEX = "^[0-9]{8}$";
	
	private final SimpleDateFormat sdf4Date = new SimpleDateFormat("yyyyMMdd");

	private final SimpleDateFormat sdf4Time = new SimpleDateFormat("HH:mm:ss");
	
	private final String basePath;
	
	public FluxStatFileExporter() {
	    this.basePath = FluxStatCollector.getCollector(null).getDir() +File.separator+"log"+ File.separator + "zdubbo" + File.separator + "flux";
    }
	
    public void export(Map<FluxStatKey, long[]> statData, long statTime) {
        long interval = FluxStatCollector.getCollector(null).getInterval();
    	Date date = new Date(statTime * interval);
	    String formattedDate = sdf4Date.format(date);
	    String formattedTime = sdf4Time.format(date);
        String application = null;
        long[] consumerStats = null;
        long[] providerStats = null;
        
        String datePath = this.basePath + File.separator + formattedDate;
        File dateDir = new File(datePath);
        if (!dateDir.exists()) {
            clearExpiredDateDirs();
            dateDir.mkdirs();
        }
        
        for (Entry<FluxStatKey, long[]> entry : statData.entrySet()) {
			FluxStatKey statKey = entry.getKey();
			long[] serviceStats = entry.getValue();
			if (serviceStats == null) {
				continue;
			}
            URL url = statKey.getUrl();
            if (application == null) {
                application = url.getParameter(FluxStatKey.APPLICATION);
            }
            
            StringBuilder fileNameBuilder = new StringBuilder(statKey.getService());
            if (StringUtils.isNotEmpty(statKey.getGroup())) {
                fileNameBuilder.append("[").append(statKey.getGroup()).append("]");
            }
            if (StringUtils.isNotEmpty(statKey.getVersion())) {
                fileNameBuilder.append("[").append(statKey.getVersion()).append("]");
            }
            String fileName = checkLength(fileNameBuilder.toString());
            String path = datePath + File.separator + application + File.separator + statKey.getSide() + File.separator + fileName + CSV_SUFFIX;
            exportToFile(path, serviceStats, formattedTime);

			if (Constants.PROVIDER_SIDE.equals(statKey.getSide())) {
			    if (providerStats == null) {
			        providerStats = new long[serviceStats.length];
                }
			    addToAppStats(providerStats, serviceStats);
            } else {
                if (consumerStats == null) {
                    consumerStats = new long[serviceStats.length];
                }
                addToAppStats(consumerStats, serviceStats);
            }
		}
		
		if (providerStats != null) {
		    String path = datePath + File.separator + application + File.separator  + checkLength(application + "[" + Constants.PROVIDER_SIDE + "]") + CSV_SUFFIX;
		    exportToFile(path, providerStats, formattedTime);
        }
		if (consumerStats != null) {
            String path = datePath + File.separator + application + File.separator  + checkLength(application + "[" + Constants.CONSUMER_SIDE + "]") + CSV_SUFFIX;
            exportToFile(path, consumerStats, formattedTime);
        }
		
	}

    private String checkLength(String fileName) {
        if (fileName.length() > Constants.FLUX_STAT_FILE_NAME_MAX_CHARS - CSV_SUFFIX.length()) {
            return fileName.substring(fileName.length() - Constants.FLUX_STAT_FILE_NAME_MAX_CHARS + CSV_SUFFIX.length(), fileName.length());
        } else {
            return fileName;
        }
    }

    private void addToAppStats(long[] appStats, long[] serviceStats) {
        appStats[0] += serviceStats[0]; // success
        appStats[1] += serviceStats[1]; // failure
        appStats[2] += serviceStats[2]; // timeout
        appStats[3] += serviceStats[3]; // elapse
        appStats[4] = appStats[4] > serviceStats[4] ? appStats[4] : serviceStats[4]; // max elapse
        appStats[5] = appStats[5] > serviceStats[6] ? appStats[5] : serviceStats[6]; // max app concurrent
        appStats[9] += serviceStats[9]; // rejection
        appStats[8] += serviceStats[8]; // overload
    }
    
    private void exportToFile(String filePath, long[] statistics, String time) {
        File file = new File(filePath);
        FileWriter writer = null;
        try {
            StringBuilder builder = new StringBuilder();
            if (!file.exists()) {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                builder.append(TITLE).append(LINE_SEPERATOR);
            }
            builder.append(time) //
                    .append(ITEM_SEPERATOR) //
                    .append(statistics[0]) //
                    .append(ITEM_SEPERATOR) //
                    .append(statistics[1]) //
                    .append(ITEM_SEPERATOR) //
                    .append(statistics[2]) //
                    .append(ITEM_SEPERATOR) //
                    .append(statistics[9]) //
                    .append(ITEM_SEPERATOR) //
                    .append(statistics[8]) //
                    .append(ITEM_SEPERATOR) //
                    .append((statistics[0] + statistics[1]) > 0 ? statistics[3] / (statistics[0] + statistics[1]) : -1) //
                    .append(ITEM_SEPERATOR) //
                    .append(statistics[4]) //
                    .append(ITEM_SEPERATOR) //
                    .append(statistics[5]);
            builder.append(LINE_SEPERATOR);
            writer = new FileWriter(file, true);
            writer.write(builder.toString());
            writer.flush();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
    
    private void clearExpiredDateDirs() {
        try {
            int retainCount = FluxStatCollector.getCollector(null).getRetainCount();
            File baseDir = new File(this.basePath);
            String[] datePaths = baseDir.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    File file = new File(dir, name);
                    Pattern pattern = Pattern.compile(DATE_REGEX);
                    Matcher matcher = pattern.matcher(name);
                    return matcher.matches() && file.isDirectory();
                }
            });

            if (null == datePaths) {
                return;
            }

            Arrays.sort(datePaths);
            
            for (int i = 0; i < datePaths.length; i++) {
                if (datePaths.length - i > retainCount) {
                    try {
                        Path start = Paths.get(this.basePath + File.separator + datePaths[i]);
                        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }

                            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                                if (e == null) {
                                    Files.delete(dir);
                                    return FileVisitResult.CONTINUE;
                                } else {
                                    throw e;
                                }
                            }
                        });
                    } catch (Exception e) {
                        logger.error("Failed to delete expired flux stat dir: " + baseDir.getAbsolutePath() + File.separator + datePaths[i], e);
                    }
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to clear expired flux stat date dirs", e);
        }
    }
}
