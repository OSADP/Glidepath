# Log Analysis Tool for Glidepath
# Kyle Rush <kyle.rush@leidos.com> 2015

require 'csv'

# Method definitions

def timestamp_to_millis(time)
  split = time.split(":")
  split[0].to_f * 60 * 60 * 1000.0 + split[1].to_f * 60 * 1000.0 + split[2].to_f * 1000.0
end

def timing_analysis
  times_count = {}
  ARGV[1..ARGV.length].each do |file|
    data = []
    puts "Opening %s and getting data..." % file
    File.open(file).each do |line|
      data << line.split("\t")
    end

    # Pull out a list of only the time step starts
    cycles = data.select { |line| line[4] =~ /Start of Consumer Cycle/ }

    # Now compute the differences between them
    for i in 1..(cycles.size - 1)
      diff = timestamp_to_millis(cycles[i][0]) - timestamp_to_millis(cycles[i - 1][0])
      times_count[diff] != nil ? times_count[diff] += 1 : times_count[diff] = 1 
    end
  end

  times = []
  times_count.keys.each do |time|
    times_count[time].times do
      times << time
    end
  end

  mean = times.reduce(:+)/times.length
  puts "Total timesteps evaluated: %d" % times.size
  puts "Average cycle time = %f" % mean
  puts "Max cycle time %f, min cycle time %f" % [times.max, times.min]
  puts "Std. Deviation: %f" % Math.sqrt((times.map { |v| (v - mean)**2 }).reduce(:+)/times.length)
  puts "CSV output of ms/cycle, num occurrences written to timing_output.csv"
  CSV.open("timing_output.csv", "w") { |csv| times_count.to_a.each { |elem| csv << elem } }

  # Log-normal distribution calculations
  times_count_ln = {}
  times_count.each_pair do | key, value |
    times_count_ln[Math.log10(key)] = value
  end

  puts ""
  puts "Log-normal calculations"
  times = times.map { |x| Math.log10(x) }
  mean = times.reduce(:+)/times.length
  stddev = Math.sqrt((times.map { |v| (v - mean)**2 }).reduce(:+)/times.length)
  puts "Mean cycle time: %f" % mean
  puts "Max: %f; Min: %f" % [times.max, times.min]
  puts "Std. Deviation: %f" % stddev
  puts "Mean - 2 sigma = %f" % 10 ** (mean - 2 * stddev)
  puts "Mean - 1 sigma = %f" % 10 ** (mean - stddev)
  puts "Mean + 1 sigma = %f" % 10 ** (mean + stddev)
  puts "Mean + 2 sigma = %f" % 10 ** (mean + 2 * stddev)
  puts "CSV output of ms/cycle, num occurrences written to timing_output_lognormal.csv"

  CSV.open("timing_output_lognormal.csv", "w") { |csv| times_count_ln.to_a.each { |elem| csv << elem } }
end



def consumer_timing_analysis
  # Split the input into parseable data
  data = []
  ARGV[1..ARGV.length].each do |file|
    puts "Opening %s and getting data..." % file
    File.open(file).each do |line|
      data << line.split("\t")
      # data[0] = timestamp
      # data[1] = log level
      # data[2] = class name
      # data[3] = tag
      # data[4] = message
    end
  end
  
  # Grab the timing entries from the data set
  times_count = {}
  data.each do |entry|
    if entry[4]=~/Full consumer read cycle in Millis: (\d*)/
      times_count[$1.to_i] != nil ? times_count[$1.to_i] += 1 : times_count[$1.to_i] = 1  
    end
  end

  # Compute statistical values
  times = []
  times_count.keys.each do |time|
    times_count[time].times do
      times << time
    end
  end

  # Normal distribution calculations
  puts "Normal calculations"
  mean = times.reduce(:+)/times.length
  puts "# of timesteps: %f" % times.length
  puts "Mean cycle time: %f" % mean
  puts "Max: %d; Min: %f" % [times.max, times.min]
  puts "Std. Deviation: %f" % Math.sqrt((times.map { |v| (v - mean)**2 }).reduce(:+)/times.length)
  puts "CSV output of ms/cycle, num occurrences written to timing_output.csv"
  CSV.open("timing_output.csv", "w") { |csv| times_count.to_a.each { |elem| csv << elem } }

  # Log-normal distribution calculations
  times_count_ln = {}
  times_count.each_pair do | key, value |
    times_count_ln[Math.log10(key)] = value
  end

  puts ""
  puts "Log-normal calculations"
  times = times.map { |x| Math.log10(x) }
  mean = times.reduce(:+)/times.length
  stddev = Math.sqrt((times.map { |v| (v - mean)**2 }).reduce(:+)/times.length)
  puts "Mean cycle time: %f" % mean
  puts "Max: %f; Min: %f" % [times.max, times.min]
  puts "Std. Deviation: %f" % stddev
  puts "Mean - 2 sigma = %f" % 10 ** (mean - 2 * stddev)
  puts "Mean - 1 sigma = %f" % 10 ** (mean - stddev)
  puts "Mean + 1 sigma = %f" % 10 ** (mean + stddev)
  puts "Mean + 2 sigma = %f" % 10 ** (mean + 2 * stddev)
  puts "CSV output of ms/cycle, num occurrences written to timing_output_lognormal.csv"

  CSV.open("timing_output_lognormal.csv", "w") { |csv| times_count_ln.to_a.each { |elem| csv << elem } }
end

# For each timestep compare time spent in consumer vs time spent outside of consumers
def consumer_comparison
  times_count = {}
  ARGV[1..ARGV.length].each do |file|
    data = []
    puts "Opening %s and getting data..." % file
    File.open(file).each do |line|
      data << line.split("\t")
    end

    # Pull out a list of only the time step starts
    cycles = data.select { |line| line[4] =~ /Start of Consumer Cycle/ }

    # Now compute the differences between them
    for i in 1..(cycles.size - 1)
      diff = timestamp_to_millis(cycles[i][0]) - timestamp_to_millis(cycles[i - 1][0])
      times_count[diff] != nil ? times_count[diff] += 1 : times_count[diff] = 1 
    end
  end

  times = []
  times_count.keys.each do |time|
    times_count[time].times do
      times << time
    end
  end

  data = []
  ARGV[1..ARGV.length].each do |file|
    puts "Opening %s and getting data..." % file
    File.open(file).each do |line|
      data << line.split("\t")
      # data[0] = timestamp
      # data[1] = log level
      # data[2] = class name
      # data[3] = tag
      # data[4] = message
    end
  end
  
  # Grab the timing entries from the data set
  times_count1 = {}
  data.each do |entry|
    if entry[4]=~/Full consumer read cycle in Millis: (\d*)/
      times_count1[$1.to_i] != nil ? times_count1[$1.to_i] += 1 : times_count1[$1.to_i] = 1  
    end
  end

  # Compute statistical values
  times1 = []
  times_count1.keys.each do |time|
    times_count1[time].times do
      times1 << time
    end
  end

  for i in 0..(times.size - 1) do
    times[i] = times[i] - times1[i]
  end

end

def ucr_score
  data = []
  puts "Opening %s and getting data..." % ARGV[1]
  File.open(ARGV[1]).each do |line|
    data << line.split("\t")
    # data[0] = timestamp
    # data[1] = log level
    # data[2] = class name
    # data[3] = tag
    # data[4] = message
  end
  
  sum = 0
  count = 0.0
  data.each do |entry|
    if entry[4] =~ /Commanded speed is (.*) m\/s. curSpeed = (.*) m\/s/
      actual = $2.to_f
      target = $1.to_f
      # Not UCR's actual formula but we get all sorts of weird NaNs and -1s if
      # we use their's verbatim
      error = (actual - target).abs/(actual.abs + target.abs)
      sum += error.nan? ? 0 : error 
      count += 1
    end
  end
  
  puts "UCR Score: %f" % (100 * (1 - (1/count) * sum))
end

def filter_errors
  data = []
  File.open(ARGV[1]).each do |line|
    data << line.split("\t")
    # data[0] = timestamp
    # data[1] = log level
    # data[2] = class name
    # data[3] = tag
    # data[4] = message
  end

  errors = []
  
  data.each do |entry|
    if entry[1] =~ /ERROR/
      errors << entry
    end
  end
  
  File.open(ARGV[1] + ".errors", "w") do |outfile|
    errors.each do |entry|
      outfile.puts "%s\t%s\t%s\t%s\t%s" % entry
    end
  end
  
end

def filter_anomalies
  threshold = ARGV[1].to_i
  anomalies = []

  ARGV[2..ARGV.length - 2].each do |file|
    data = []
    puts "Opening %s and getting data..." % file
    File.open(file).each do |line|
      data << line.split("\t").push(file)
      # data[0] = timestamp
      # data[1] = log level
      # data[2] = class name
      # data[3] = tag
      # data[4] = message
      # data[5] = filename
    end

    # Pull out a list of only the time step starts
    cycles = data.select { |line| line[4] =~ /Start of Consumer Cycle/ }

    # Now compute the differences between them
    for i in 1..(cycles.size - 1)
      diff = timestamp_to_millis(cycles[i][0]) - timestamp_to_millis(cycles[i - 1][0])
      if diff > threshold
        anomalies << (cycles[i] << diff.to_s)
      end
    end
  end

  puts "Writing %d anomalies to output file %s." % [ anomalies.length, ARGV[ARGV.length - 1]]
  File.open(ARGV[ARGV.length - 1], "w") do |outfile|
    anomalies.each do |entry|
      outfile.puts "%s\t%s\t%s\t%s\t%s\t%s\t%s" % entry
    end
  end
end

################################################
# Begin Main Program Body
################################################

# Decide mode of operation
mode = ARGV[0]
case mode
when "-timing"
  timing_analysis
when "-errors"
  filter_errors
when "-ucrscore"
  ucr_score
when "-anomalies"
  filter_anomalies
when "-consumer"
  consumer_timing_analysis
when "-h"
  puts <<HELP_MESSAGE
Glidepath Log Analysis Tool
Valid arguments:
  -timing <input_files>: Statistical analysis of cycle timing data in both norma   l and log-log normal modes.
  -ucrscore <input_file>: Analysis of commanded vs actual speed data
  -errors <input_file>: Filter out error level messages only, save in <input_fil   e>.errors
  -anomalies <threshold> <input_files> <output_file>: Filter out all timesteps     with duration longer than threshold. Write to output_file.
HELP_MESSAGE
else
  puts "Unrecognized argument %s. Try -h for options." % mode
end
