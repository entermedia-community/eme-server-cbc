package users;

import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.*
import org.openedit.users.Group
import org.openedit.users.GroupSearcher
import org.openedit.users.User
import org.openedit.users.UserSearcher
import org.openedit.util.Exec
import org.openedit.util.ExecResult
import org.openedit.util.PathUtilities



public void init()
{
//ldapsearch -Z -LLL -E pr=200/noprompt -H ldaps://mtlwinads30.cbc-rc.ca -D 'cbcrc\emshare' -w '3m5har3!' -b "DC=cbc-rc, DC=ca" -s sub objectClass=user uid mail givenName sn memberOf + > /opt/entermediadb/webapp/WEB-INF/bin/linux/users.txt

	        MediaArchive archive = context.getPageValue("mediaarchive");
		Exec exec = (Exec)moduleManager.getBean("exec");
		ArrayList command = new ArrayList();

		ExecResult found  = exec.runExec("/media/services/tools/ldapuserexport.sh", command, true);

		UserSearcher usersearch = archive.getSearcherManager().getSearcher("sources/catalog","user");
		GroupSearcher groupsearch = archive.getSearcherManager().getSearcher("sources/catalog","group");


//on cree nos objets de groupes
		Group admingroup = createGroup(groupsearch,"CBC RC MTL GG EMShare Admin Group");
		Group editgroup = createGroup(groupsearch,"DTV MTL GG EMS SOURCES EDIT");
		Group viewgroup = createGroup(groupsearch,"DTV MTL GG EMS SOURCES VIEW");
		Group corpgroup = createGroup(groupsearch,"DTV MTL GG EMS SOURCES CORP");
		Group archrechgroup = createGroup(groupsearch,"EMS NAT GG ARCH RECH");
		Group archtraitgroup = createGroup(groupsearch,"EMS NAT GG ARCH TRAIT");
        Group testgroup = createGroup(groupsearch,"DTV MTL GG EMS SOURCES TEST");


		Searcher profilesearcher =archive.getSearcherManager().getSearcher(archive.getCatalogId(), "userprofile");

		BufferedReader read = new BufferedReader(new FileReader("/opt/entermediadb/webapp/WEB-INF/bin/linux/users.txt"));
		try
		{
		 User current = null;
		 for(String line; (line = read.readLine()) != null; )
		 {
			 if( line.startsWith("dn: CN=") )
			 {
				 String userid = line.substring(7,line.indexOf(","));
				// log.info("Found ${userid}");
								 userid = userid.toLowerCase();

				 if( current != null )
				 {
					//TODO: Make a function to save current user and make sure I call it once at the end
					//Is this a group we care about?
					if( current.isInGroup(admingroup)
						|| current.isInGroup(editgroup)
						|| current.isInGroup(viewgroup)
						|| current.isInGroup(corpgroup)
                        || current.isInGroup(testgroup)
						|| current.isInGroup(archtraitgroup)
						|| current.isInGroup(archrechgroup))
					 {
						 usersearch.saveData(current,null);
												// log.info("Saved ${current.getId()}");
										 Data profile = (Data) profilesearcher.searchById(current.getUserName());
												if( profile == null )
												{
													  profile = profilesearcher.createNewData();
													  profile.setProperty("userid",current.getId());
													   profile.setId(current.getId());
												 }
												 if( current.isInGroup(admingroup) )
												 {
														  profile.setProperty("settingsgroup","administrator");
													   //   log.info("Made admin");
												 }
												 else if( current.isInGroup(editgroup) )
												 {
														  profile.setProperty("settingsgroup","AV_1Mq7p-sozj__BfdF_");
												 }
												 else if( current.isInGroup(viewgroup) )
												 {
														  profile.setProperty("settingsgroup","AV_12Y-A-sozj__BfdGJ");
												 }
												 else if( current.isInGroup(corpgroup) )
												 {
														  profile.setProperty("settingsgroup","AWJHpPcZTOdBKZqUMM1f");
												 }
												else if( current.isInGroup(testgroup) )
												{
														  profile.setProperty("settingsgroup","AWZYlVf5gMfLKVPznUJG");
												}
												else if( current.isInGroup(archtraitgroup) )
												{
														  profile.setProperty("settingsgroup","AW-i6KZmMdL76uoJ89E-");
												}
												else if( current.isInGroup(archrechgroup) )
												{
														  profile.setProperty("settingsgroup","AW3Qh72og1N012eDhpiK");
												}

												log.info("saved profile ${profile.getId()} as ${profile.settingsgroup}");
												profilesearcher.saveData(profile,null);
					 }
				 }
				 current = usersearch.getUser(userid);
				 if( current == null)
				 {
					 current = (User)usersearch.createNewData();
										 current.setId(userid);
				 }
								 //TODO: Save any existing users in old groups
                                 current.setEnabled(true);
				 current.removeGroup(admingroup);
				 current.removeGroup(editgroup);
				 current.removeGroup(viewgroup);
				 current.removeGroup(corpgroup);
                 current.removeGroup(testgroup);
				 current.removeGroup(archrechgroup);
				 current.removeGroup(archtraitgroup);
			 }
			 if( line.startsWith("memberOf: CN=") && current != null)
			 {
				 if( line.contains(admingroup.getName()))
				 {
					 current.addGroup(admingroup);
				 }
				 else if( line.contains(editgroup.getName()))
				 {
					 current.addGroup(editgroup);
				 }
				 else if( line.contains(viewgroup.getName()))
				 {
					 current.addGroup(viewgroup);
				 }
				 else if( line.contains(corpgroup.getName()))
				 {
					 current.addGroup(corpgroup);
				 }
                 else if( line.contains(testgroup.getName()))
				 {
					 current.addGroup(testgroup);
				 }
				 else if( line.contains(archtraitgroup.getName()))
				 {
					 current.addGroup(archtraitgroup);
				 }
				 else if( line.contains(archrechgroup.getName()))
				 {
					 current.addGroup(archrechgroup);
				 }

			 }
			 else if( line.startsWith("mail: "))
			 {
				 current.setEmail(line.substring(6,line.length()));
			 }
			 else if( current != null && line.startsWith("sn: "))
			 {
				 current.setLastName(line.substring(3,line.length()));
			 }
			 else if( line.startsWith("givenName: "))
			 {
				 current.setFirstName(line.substring(10,line.length()));
			 }
			}
		}
		finally
		{
			read.close();
		}
}

public Group createGroup(GroupSearcher inGroups, String inName)
{
	String id = PathUtilities.extractId(inName);
	Group found = inGroups.getGroup(id);
	if( found == null )
	{
	 found = inGroups.createNewData();
	 found.setId(id);
	 found.setName(inName);
	 inGroups.saveData(found,null);
   }
   return found;
}
init();
