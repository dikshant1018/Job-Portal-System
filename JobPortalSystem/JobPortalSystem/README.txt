JobPortal System (upgraded)
===========================
Run in IntelliJ: open this folder, keep your mysql-connector-j library, run com.jobportal.ui.MainApp.
Run without IntelliJ: copy mysql-connector-j jar into lib/, then run.bat (Windows) or ./run.sh.

Database: same Railway MySQL (db/DBConnection.java is unchanged). On first start DBInit upgrades
the old tables automatically (adds profiles, documents, saved jobs; hashes old passwords; removes
duplicate applications). Back up the Railway data first if you need the old rows.

Default admin: admin@jobportal.com / admin123  (change it in Account Settings)

Optional nicer look: add the FlatLaf jar (flatlaf-3.x.jar) to lib/ / project libraries.
