from fabric.api import local, settings, abort, run, env, hide, roles, cd, sudo
from fabric.contrib.console import confirm
from fabric.operations import *
from fabric.colors import *
import re
import inspect
from contextlib import closing
from zipfile import ZipFile, ZIP_DEFLATED
import os


env.disable_known_hosts = True
env.num_backups = 10 # number of deployment backups to keep around.
env.backup_dir = 'cheshire_backups' # name of directory to hold deployment backups
env.jar_name = 'cheshireproject.jar' 
env.run_as_user = 'trendrr' #the user that the server process should run as
env.api_docs = True # include strestdoc?
env.config_dir = '' 
env.deploy_base_dir = '/mnt/trendrr'
env.jvm_args = '-Xmx256m' # arguments to pass the jvm
env.app_args = '' # arguments to pass to the app

def development():
	env.hosts = ['fillmein']
	env.api_docs = True
	# should include this line
	env.user = raw_input(green('Please enter your username for the server:'))
	env.to_deploy = 'CheshireProject'
	env.config_dir = 'config/' + inspect.getframeinfo(inspect.currentframe()).function



def production():
	val = raw_input(green("Which instance? ") + red("node1, node2, node3") + green("? "))
	if val == 'node1' :	
		env.hosts = ['fillmein']
	elif val == 'node2' :
		env.hosts = ['fillmein']
	elif val == 'node3' :
		env.hosts = ['fillmein']
	
	# should include this
	env.user = raw_input(green('Please enter your username for the server:'))
	env.to_deploy = 'CheshireProject'
	env.config_dir = 'config/' + inspect.getframeinfo(inspect.currentframe()).function


def _backup():
	# Backs up the current deploy	
	# 
	# create backups directory if needed
	sudo('mkdir -p %s/%s/%s' % (env.deploy_base_dir,env.backup_dir,env.to_deploy), user=env.run_as_user)
	
	#create the directories in case they don't exist.  avoid a bunch of scary warning during the mv
	for index in reversed(range(0,env.num_backups)):
		sudo('mkdir -p %s/%s/%s/backup_%d' % (env.deploy_base_dir,env.backup_dir,env.to_deploy, index), user=env.run_as_user)
	
	#rename the old backups
	with settings(warn_only=True):
		sudo('rm -R %s/%s/%s/backup_%d' % (env.deploy_base_dir,env.backup_dir,env.to_deploy,env.num_backups))
		for index in reversed(range(0,env.num_backups)):
			print('mv %s/%s/%s/backup_%d %s/%s/%s/backup_%d' % (env.deploy_base_dir, env.backup_dir,env.to_deploy,index,env.deploy_base_dir,env.backup_dir,env.to_deploy,index+1))
			sudo('mv %s/%s/%s/backup_%d %s/%s/%s/backup_%d' % (env.deploy_base_dir,env.backup_dir,env.to_deploy,index,env.deploy_base_dir,env.backup_dir,env.to_deploy,index+1))
		
		sudo('mv %s/%s %s/%s/%s/backup_0' % (env.deploy_base_dir,env.to_deploy,env.deploy_base_dir,env.backup_dir,env.to_deploy))	
	

'''
	Attempts to stop and restart the process
'''	
def stop():
	# this is a bad to stop the process, but it *always* works ;)
	temp = run('ps -ef | grep -F "%s %s"' % (env.jar_name,env.app_args))
	if not temp:
		print "No current process running, skipping"
		return
	temp = re.findall(r'%s\s+[0-9]+' % env.run_as_user, temp)
	if not temp:
		print "No current process running, skipping"
		return
	for t in temp :
		pid = re.search(r'[0-9]+', t).group()
		sudo('kill -9 %s' % pid)	
	service_remove(env.to_deploy)


def start():
	with cd('%s/%s' % (env.deploy_base_dir, env.to_deploy)):
		cmd = 'nohup java -jar %s %s %s > /dev/null' % (env.jvm_args,env.jar_name,env.app_args)
		service_create(env.to_deploy, cmd)		
		sudo(cmd, user=env.run_as_user)

def restart():
	stop()
	start()

def service_create(name, command):
    # creates a new service that can start/stop and automatically starts on server reboot.
    service_remove(name)
    sudo('touch /etc/init/%s.conf' % name)
    conf = '''
    #
    # Cheshire service deployed from fab
    #
    start on (net-device-up
              and local-filesystems
              and runlevel [2345])
    script
		cd %s/%s
        sudo -u %s %s
    end script
    ''' % (env.deploy_base_dir, name, env.run_as_user, command)
    sudo('echo "%s" | sudo tee -a /etc/init/%s.conf' % (conf,name))

def service_remove(name):  
    sudo('rm -f /etc/init/%s.conf' % name)

'''
	Desplays the log file
'''
def log():
	sudo('tail -f -n 150 %s/%s/logs/api.log' % (env.deploy_base_dir, env.to_deploy))	

'''
	Displays the access log
'''
def access_log():
	sudo('tail -f -n 150 %s/%s/logs/access.log' % (env.deploy_base_dir, env.to_deploy))

def deploy():	
	# we make the mnt directory so its there for the new ebs instances.
	with settings(warn_only=True):
		sudo('mkdir %s && sudo chown -R %s:%s %s' % (env.deploy_base_dir, env.run_as_user,env.run_as_user,env.deploy_base_dir))

	if env.num_backups > 0 :
		_backup()
	
	sudo('mkdir -p %s/%s/logs' % (env.deploy_base_dir, env.to_deploy))
	sudo('mkdir -p %s/%s/config' % (env.deploy_base_dir, env.to_deploy))

	upload_dir('lib', '%s/%s/lib' % (env.deploy_base_dir, env.to_deploy))
	if env.api_docs :
		upload_dir('strestdoc', '%s/%s/strestdoc' % (env.deploy_base_dir, env.to_deploy))

	upload_dir('static', '%s/%s/static' % (env.deploy_base_dir, env.to_deploy))
	upload_dir('', '%s/%s/views' % (env.deploy_base_dir, env.to_deploy))
	upload_dir('%s' % env.config_dir, '%s/%s/config' % (env.deploy_base_dir, env.to_deploy))

	put('%s' % (env.jar_name),'%s/%s/%s' % (env.deploy_base_dir, env.to_deploy, env.jar_name), use_sudo=True)
		
	_blame_me_file('%s/%s' % (env.deploy_base_dir, env.to_deploy))

	sudo('chown -R %s:%s %s/%s' % (env.run_as_user,env.run_as_user,env.deploy_base_dir, env.to_deploy))
	sudo('chmod -R g+w %s/%s' % (env.deploy_base_dir, env.to_deploy))

	sudo('echo "%s" | sudo tee -a %s/%s/host' % (env.host, env.deploy_base_dir, env.to_deploy))
	
	stop()
	start()


def _blame_me_file(file_dir) :
	from time import strftime
	from datetime import datetime 
	from pytz import timezone
	 # blame me (lists username and date of when the dploy happened)
	now = datetime.now(timezone("US/Eastern"))
	sudo('echo "Deployed by\nUser: %s\nOn: %s" | sudo tee -a %s/BlameMe.txt' % (env.user, now.strftime("%m-%d-%Y %I:%M:%S %p %Z"), file_dir ))

'''
	Zips a local directory uploads, then unzips
'''
def upload_dir(fm, to, local_tmp='tmp', remote_tmp='/tmp'):
	if not zipdir(fm, local_tmp + os.sep + 'fab_tmp.zip') :
		return #can't upload since zip failed!		
	put(local_tmp + os.sep + 'fab_tmp.zip',remote_tmp, use_sudo=True)
	with cd(remote_tmp) :	
		sudo('mkdir -p %s' % to)
		sudo('unzip -o fab_tmp.zip -d %s' % to)
		sudo('rm fab_tmp.zip')


'''
	Zips a directory locally
'''
def zipdir(basedir, archivename):
	print archivename
	try:
		os.makedirs(archivename) # make the directories if needed
		os.rmdir(archivename) #remove the filename 
	except:
		pass
	
	try:
		os.rm(archivename)
	except:
 		pass
	if not os.path.isdir(basedir) :
		print 'unable to upload directory: %s as it does not exist!' % basedir
		return False

	with closing(ZipFile(archivename, "w", ZIP_DEFLATED)) as z:
		for root, dirs, files in os.walk(basedir):
			#NOTE: ignore empty directories
			for fn in files:
				absfn = os.path.join(root, fn)
				zfn = absfn[len(basedir)+len(os.sep):] #XXX: relative path
				print zfn
				z.write(absfn, zfn)
	return True
