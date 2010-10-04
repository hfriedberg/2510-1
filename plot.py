#!/usr/bin/python

# Yann Le Gall
# ylegall@gmail.com
#
# script that reads a text file whose lines 
# have the following format:
#
# startTime,stopTime,pID,action
#

# open the input and output files
input = open('out.txt', 'r')
output = open('graph.html','w')

actions = {} # a dictionary to hold actions

# parse the input file:
maxTime = 0
for line in input:
	if len(line.strip()) == 0:
		continue
	tokens = [item.strip() for item in line.split(',')]
	if int(tokens[1]) > maxTime:
		maxTime = int(tokens[1])
	if tokens[2] not in actions:
		actions[tokens[2]] = []
	actions[tokens[2]].append( (tokens[0], tokens[1], tokens[3]) )
		
input.close()

# debug
for (key,val) in actions.items():
	print key, val

# create the output html file
output.write('''
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<title>cs2510 graph</title>

<style type="text/css">
body {padding-left:40px;}
table
{
	border: thin solid black;
	border-collapse:collapse;
}

td
{
    width:25px;
    height:25px;
    text-align:center;
    border-top: thin solid gray;
	padding: 5px;
}

th {
	border-left: thin solid gray;
	padding:5px;
}

td.r
{
	background-color:#6699FF;
	color:blue;
}

td.w
{
	background-color:#FF9933;
	color:red;
}
td.p
{
	font-weight:bold;
	border-right:thin solid black;
	width:auto;
}
</style>

<body>
	<br/>
''')	

# write the process task
output.write('<br><h3>Process Workload</h3>')
output.write('<pre>')
for i in range(1, len(actions) + 1):
	output.write('process %s: ' % str(i))
	output.write(str(actions[str(i)]))
	output.write('\n')
output.write('</pre>')

output.write('<br><h3>System Timeline</h3>')

# write the table header
output.write('<table>\n<tr><th></th>')
for i in range(maxTime):
	output.write('<th>')
	output.write(str(i + 1))
	output.write('</th>')
output.write('</tr>\n')

# write the actions for each process
for i in range(1,len(actions)+1):
	actionList = actions[str(i)]
	output.write('<tr><td class="p">process %d</td>' % i )
	t = 1
	while t <= maxTime:
		try:
			task = actionList[0]
		except IndexError:
			task=('0','0','i')
		output.write('<td')
		if t >= int(task[0]) and t < int(task[1]):
			if task[2] == 'read':
				output.write(' class="r">')
			else:
				output.write(' class="w">')
			output.write(task[2][0])
		else:
			output.write('>')
		output.write('</td>')
		
		if t == int(task[1]):
			actionList.pop(0)
#			continue
		t += 1

	output.write('</tr>\n')
output.write('</table>\n</body></html>')

output.close()
