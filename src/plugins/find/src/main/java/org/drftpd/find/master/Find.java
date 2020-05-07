/*
 * This file is part of DrFTPD, Distributed FTP Daemon.
 *
 * DrFTPD is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * DrFTPD is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * DrFTPD; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston, MA 02111-1307 USA
 */
package org.drftpd.find.master;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.drftpd.common.misc.CaseInsensitiveHashMap;
import org.drftpd.common.util.Bytes;
import org.drftpd.find.master.action.ActionInterface;
import org.drftpd.find.master.option.OptionInterface;
import org.drftpd.master.GlobalContext;
import org.drftpd.master.commands.*;
import org.drftpd.master.indexation.AdvancedSearchParams;
import org.drftpd.master.indexation.IndexEngineInterface;
import org.drftpd.master.indexation.IndexException;
import org.drftpd.master.network.Session;
import org.drftpd.master.usermanager.User;
import org.drftpd.master.vfs.DirectoryHandle;
import org.drftpd.master.vfs.FileHandle;
import org.drftpd.master.vfs.InodeHandle;
import org.reflections.Reflections;

import java.io.FileNotFoundException;
import java.util.*;


/**
 * @author pyrrhic
 * @author mog
 * @author fr0w
 * @author scitz0
 * @version $Id$
 */
public class Find extends CommandInterface {
    public static final Logger logger = LogManager.getLogger(Find.class);

    private ResourceBundle _bundle;


    private final CaseInsensitiveHashMap<String, OptionInterface> _optionsMap = new CaseInsensitiveHashMap<>();
    private final CaseInsensitiveHashMap<String, ActionInterface> _actionsMap = new CaseInsensitiveHashMap<>();

    public void initialize(String method, String pluginName, StandardCommandManager cManager) {
        super.initialize(method, pluginName, cManager);
        _bundle = cManager.getResourceBundle();


        // Subscribe to events
        AnnotationProcessor.process(this);

        // TODO [DONE] @k2r Load all options and actions
        // Load all options
        Set<Class<? extends OptionInterface>> options = new Reflections("org.drftpd")
                .getSubTypesOf(OptionInterface.class);
        try {
            for (Class<? extends OptionInterface> option : options) {
                OptionInterface optionInterface = option.getConstructor().newInstance();
                _optionsMap.put("-" + option.getSimpleName().replace("Option", ""), optionInterface);
            }
        } catch (Exception e) {
            logger.error("Failed to load options for org.drftpd.master.commands.find extension point 'Option'"
                    + ", possibly the org.drftpd.master.commands.find"
                    + " extension point definition has changed in the plugin.xml", e);
        }

        Set<Class<? extends ActionInterface>> actions = new Reflections("org.drftpd")
                .getSubTypesOf(ActionInterface.class);
        try {
            for (Class<? extends ActionInterface> action : actions) {
                ActionInterface actionInterface = action.getConstructor().newInstance();
                _actionsMap.put("-" + actionInterface.name(), actionInterface);
            }
        } catch (Exception e) {
            logger.error("Failed to load options for org.drftpd.master.commands.find extension point 'Action'"
                    + ", possibly the org.drftpd.master.commands.find"
                    + " extension point definition has changed in the plugin.xml", e);
        }
    }

    public CommandResponse doFIND(CommandRequest request) throws ImproperUsageException {
        if (!request.hasArgument()) {
            throw new ImproperUsageException();
        }

        AdvancedSearchParams params = new AdvancedSearchParams();

        DirectoryHandle dir = request.getCurrentDirectory();

        User user = request.getSession().getUserNull(request.getUser());

        ArrayList<ActionInterface> actions = new ArrayList<>();

        int limit = Integer.parseInt(request.getProperties().getProperty("limit.default", "5"));
        int maxLimit = Integer.parseInt(request.getProperties().getProperty("limit.max", "20"));

        boolean quiet = false;

        LinkedList<String> args = new LinkedList<>(Arrays.asList(request.getArgument().split("\\s+")));

        if (args.isEmpty()) {
            throw new ImproperUsageException();
        }

        while (!args.isEmpty()) {
            String arg = args.poll();

            if (arg.equalsIgnoreCase("-quiet")) {
                quiet = true;
            } else if (arg.equalsIgnoreCase("-limit")) {
                if (args.isEmpty()) {
                    throw new ImproperUsageException();
                }
                try {
                    int newLimit = Integer.parseInt(args.poll());
                    if (newLimit > 0 && newLimit < maxLimit) {
                        limit = newLimit;
                    } else {
                        limit = maxLimit;
                    }
                } catch (NumberFormatException e) {
                    throw new ImproperUsageException("Limit must be valid number.");
                }
            } else if (arg.equalsIgnoreCase("-section")) {
                if (args.isEmpty()) {
                    throw new ImproperUsageException();
                }
                dir = GlobalContext.getGlobalContext().getSectionManager().
                        getSection(args.poll()).getBaseDirectory();
            } else if (_optionsMap.containsKey(arg)) {
                // Check if arg matches any of the loaded options
                try {
                    OptionInterface option = _optionsMap.get(arg);
                    option.exec(arg, getArgs(args), params);
                } catch (Exception e) {
                    logger.debug("Option = {}", arg, e);
                    return new CommandResponse(500, e.getMessage());
                }
            } else if (_actionsMap.containsKey(arg)) {
                // Check if arg matches any of the loaded actions
                if (!checkCustomPermission(request, arg.toLowerCase(), "*")) {
                    return new CommandResponse(500, "You do not have the proper permissions for " + arg);
                }
                try {
                    ActionInterface action = _actionsMap.get(arg);
                    action.initialize(arg, getArgs(args));
                    actions.add(action);
                } catch (Exception e) {
                    logger.debug("Action = {}", arg, e);
                    return new CommandResponse(500, e.getMessage());
                }
            } else {
                // Switch doesn't exist
                throw new ImproperUsageException();
            }
        }

        if (actions.isEmpty()) {
            throw new ImproperUsageException();
        }

        params.setLimit(0); // Get all results, we filter out hidden inodes later

        IndexEngineInterface ie = GlobalContext.getGlobalContext().getIndexEngine();
        Map<String, String> inodes;

        try {
            inodes = ie.advancedFind(dir, params, "doFIND");
        } catch (IndexException e) {
            logger.error(e.getMessage());
            return new CommandResponse(550, e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.info(e.getMessage());
            return new CommandResponse(550, e.getMessage());
        }

        Map<String, Object> env = new HashMap<>();

        Session session = request.getSession();

        CommandResponse response = new CommandResponse(200, "Find complete!");

        if (inodes.isEmpty()) {
            response.addComment(session.jprintf(_bundle, "find.empty", env, user.getName()));
            return response;
        }

        LinkedList<String> responses = new LinkedList<>();
        int results = 0;
        boolean observePrivPath = request.getProperties().
                getProperty("observe.privpath", "true").equalsIgnoreCase("true");

        InodeHandle inode;
        for (Map.Entry<String, String> item : inodes.entrySet()) {
            if (results == limit)
                break;
            try {
                inode = item.getValue().equals("d") ? new DirectoryHandle(item.getKey().
                        substring(0, item.getKey().length() - 1)) : new FileHandle(item.getKey());
                if ((observePrivPath && inode.isHidden(user)) || (!observePrivPath && inode.isHidden(null))) {
                    continue;
                }
                env.put("name", inode.getName());
                env.put("path", inode.getPath());
                env.put("owner", inode.getUsername());
                env.put("group", inode.getGroup());
                env.put("size", Bytes.formatBytes(inode.getSize()));
                for (ActionInterface action : actions) {
                    if ((inode.isFile() && action.execInFiles()) ||
                            (inode.isDirectory() && action.execInDirs())) {
                        logger.debug("Action {} executing on {}", action.getClass(), inode.getPath());
                        String text = action.exec(request, inode);
                        if (!quiet || action.failed())
                            responses.add(text);
                    }
                }
                results++;
            } catch (FileNotFoundException e) {
                logger.warn("Index contained an unexistent inode: {}", item.getKey());
            }
        }

        if (results == 0) {
            response.addComment(session.jprintf(_bundle, "find.empty", env, user.getName()));
            return response;
        }

        env.put("limit", limit);
        env.put("results", results);
        response.addComment(session.jprintf(_bundle, "find.header", env, user.getName()));

        for (String line : responses) {
            response.addComment(line);
        }

        return response;
    }

    /**
     * Gets all arguments for this option/action. Multiple args must be enclosed with '"'.
     * <p>
     * The <tt>LinkedList</tt> containing all arguments.
     *
     * @return A String array containing all arguments belonging to this option/action.
     * Null if LinkedList does not contain any more elements or if next element
     * starts with the '-' character.
     * @throws ImproperUsageException Thrown if end '"' is missing.
     */
    private String[] getArgs(LinkedList<String> text) throws ImproperUsageException {
        if (text.isEmpty() || text.peek().startsWith("-")) {
            return null;
        }
        String args = text.poll();
        if (args.charAt(0) == '"') {
            args = args.substring(1);
            while (true) {
                if (args.endsWith("\"")) {
                    args = args.substring(0, args.length() - 1);
                    break;
                } else if (text.isEmpty()) {
                    throw new ImproperUsageException();
                } else {
                    args += " " + text.poll();
                }
            }
        }
        return args.split(" ");
    }
}
