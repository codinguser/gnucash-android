Change Log
===============================================================================
Version 2.1.5 *(2017-04-01)*
----------------------------
* Fixed: Widget button for placeholder accounts tries to create transactions 
* Fixed: App crashes when screen orientation changes while viewing reports (#633)
* Fixed: Resource leak after termination of first-run wizard
* Fixed: OFX exporter creates file even when there are no transactions to export
* Improved: Upgrade to Dropbox API v2 (v1 will be deprecated soon) (#552)
* Improved: Use FileProvider for sharing files with other applications (#568)
* Improved: Tell user when there are no transactions to export 
* Improved: Added option to hide account balance in widget (#600)
* Improved: List transfer accounts starting with favorites first (#622)
* Improved: Management of preferences for widgets and support for multibook widgets
* Improved: Updated translations


Version 2.1.4 *(2017-01-30)*
----------------------------
* Fixed: Bugs in execution of some scheduled actions (#604, #609)
* Fixed: Multi-currency transactions not exported when format is QIF (#571)
* Fixed: Incorrect date of last export shown in book manager (#615, #617)
* Fixed: Large exports may be reported as successful even if they didn't complete yet (#616)
* Fixed: Custom date range (in reports) does not select correct ending date (#611)
* Fixed: Account color reset when editing an account (#620)
* Fixed: Export to OwnCloud fails if folder already exists
* Fixed: User not notified if export to external target fails
* Improved translations


Version 2.1.3 *(2016-10-20)*
----------------------------
* Fixed: Scheduled exports execute too often or not at all in some cases
* Fixed: Crash if device is rotated during first-run wizard execution
* Fixed: Negative values displayed as green on homescreen widget
* Improved: Homescreen widget now allows to select the book to use
* Improved: Update Russian translation

Version 2.1.2 *(2016-09-21)*
----------------------------
* Fixed: Scheduled exports always run daily (no matter the actual schedule)
* Fixed: New transactions cannot be saved in single-entry mode
* Fixed: ownCloud connect success messages displayed in red (now green)
* Fixed: Crash when scheduled action service cannot find transaction in db
* Improved: German and Brazilian Portuguese translations

Version 2.1.1 *(2016-09-05)*
----------------------------
* Fixed: Bug cause crash during start-up for devices with no scheduled transactions

Version 2.1.0 *(2016-09-01)*
----------------------------
* Feature: Use multiple GnuCash books in single application
* Feature: Backup/Export to ownCloud servers
* Feature: Compact transactions list view for single-entry mode
* Improved: Redesign of passcode screen with included alphabet keys
* Improved: Scheduled transactions now have more accurate timestamps
* Improved: Generate all scheduled transactions even if a scheduled is missed (e.g. device off)
* Improved: Updated translations (and extracted some hard-coded strings)
* Fixed: Accounts lists not properly refreshed after switching between recent and all
* Fixed: Inaccurate execution of some scheduled transactions

Version 2.0.7 *(2016-05-05)*
----------------------------
* Fixed: Currency exchange rate does not accept very small rates (> 2 decimal places)
* Improved: Updated translations for Japanese, Polish, French,

Version 2.0.6 *(2016-02-20)*
----------------------------
* Fixed: Saving transaction gets slower with increase in size of database
* Fixed: Imbalance amount wrongly computed in split editor (for some accounts)
* Fixed: Amount text boxes in split editor sometimes do not get focus
* Fixed: Crash when saving account with no transfer account selected
* Fixed: Crash when creating a new transaction with no transfer account
* Fixed: All transactions are always exported for some time zones
* Improved: Add translation for Japanese. Updated Italian and Russian

Version 2.0.5 *(2015-12-12)*
----------------------------
* Fixed: Wrong decimal formatting in multi-currency transactions
* Improved: Reliability of exports

Version 2.0.4 *(2015-12-02)*
----------------------------
* Fixed: Transaction export time not always working reliably
* Fixed: Renaming account causes transactions to be deleted
* Fixed: Progress dialog not displayed during initial import
* Fixed: Unable to finish first-run wizard if choosing to create accounts manually
* Fixed: Removed inapplicable options in scheduled actions context menu

Version 2.0.3 *(2015-11-21)*
----------------------------
* Fixed: Unable to enter decimal amounts in split editor
* Fixed: Split editor shows wrong imbalance when editing transaction
* Fixed: Auto-backups not correctly generated

Version 2.0.2 *(2015-11-20)*
----------------------------
* Fixed: Exporting to external service does not work in some devices
* Fixed: Bar chart does not display negative amounts
* Fixed: Crash when saving transaction with invalid amount expression
* Fixed: Crash when displaying bar chart legend with accounts of same name
* Fixed: Crashes when importing some GnuCash XML files on some devices
* Improved: Remember last export destination
* Improved: Display current imbalance in split editor
* Improved: Set default commodity to the one used by imported file
* Improved: Add support for unlimited fractional digits in commodities
* Improved: Option to select date from which to export transactions

Version 2.0.1 *(2015-11-05)*
----------------------------
* Feature: Menu options for moving/duplicating transactions
* Fixed: Invalid QIF exported, causing crashes when importing on desktop
* Fixed: Account delete dialog not displaying properly / only partially deleting transactions
* Fixed: Moving transaction to another account from within the split editor sets the amount to zero
* Improved: Amounts now use standard commodities & fraction digit on all devices

Version 2.0.0 *(2015-11-01)*
----------------------------
* Feature: Updated app design to use Material Design guidelines
* Feature: Setup wizard on first run of the application
* Feature: Support for multi-currency transactions
* Feature: New report summary page and more options for display/grouping reports
* Feature: Calculator keyboard when entering transactions
* Feature: Use appropriate decimal places per currency
* Feature: New help & feedback section with UserVoice
* Feature: New transaction detail view with running account balance
* Feature: Export/import commodity prices to/from GnuCash XML
* Feature: Prompt for rating the application after a number of starts
* Feature: Support for Android M permissions model
* Feature: New horizontal layout for account and transaction lists
* Feature: Automatic sending of crash reports with user permission (opt-in)
* Feature: Default transfer account setting propagates to child accounts
* Feature: Export transactions from a particular date
* Improved: Transactions are always balanced at the database layer before saving
* Improved: OFX export do not try to support double entry anymore
* Improved: Restructured the app settings categories
* Improved: Highlight active scheduled actions
* Improved: Restructured navigation drawer and added icons
* Improved: Currencies are listed sorted by currency code
* Improved: Show relative time in transaction list
* Improved: Added Portuguese translation
* Improved: Account balances are now computed faster (in parallel)
* Fixed: Data leak through app screenshot when passcode is set
* Fixed: Some inconsistencies when importing GnuCash XML
* Fixed: "Save" and "Cancel" transaction buttons not displayed in Gingerbread
* Fixed: Word-wrap on transaction type switch
* Fixed: Crash when restoring backups with poorly formatted amount strings

Version 1.6.4 *(2015-08-12)*
----------------------------
* Fixed: Crashes during backup restoration

Version 1.6.3 *(2015-08-09)*
----------------------------
* Fixed: Transfer account ignored when saving transaction with one split (after opening split editor)
* Fixed: Crash when exporting scheduled transactions when there are scheduled backups present
* Added: Polish translation

Version 1.6.2 *(2015-07-16)*
----------------------------
* Fixed: Editing accounts causing the account's transactions to be deleted

Version 1.6.1 *(2015-07-08)*
----------------------------
* Fixed: Crash when importing some scheduled transations with custom period strings
* Fixed: Crash when closing export progress dialog if an export error occurred
* Fixed: Crash when creating a sub-account and changing the account type
* Fixed: Crash when loading backup files with no timestamp in their name
* Fixed: Crash when app is run on devices with locale es_LG
* Improved: Updated betterpickers library
* Improved: New dialogs for time and date when creating transactions
* Improved: Added translation to Ukrainian

Version 1.6.0 *(2015-06-20)*
----------------------------
* Feature: Scheduled backups (QIF, OFX and XML)
* Feature: More recurrence options for scheduled transactions
* Feature: Backup/Export to DropBox and Google Drive
* Feature: Reports of income and expenses over time - bar, pie and line charts
* Feature: Import scheduled transactions from GnuCash XML (experimental)
* Feature: Set app as handler for .gnucash and .gnca files
* Feature: Auto-balance transactions before saving
* Feature: Navigation drawer for easier access to features
* Feature: Options for explicitly creating/restoring backups
* Feature: Support for hidden accounts
* Feature: Account delete dialog now has options for moving sub-accounts and transactions
* Feature: Export to Gnucash desktop-compatible XML
* Feature: Support for logging to Crashlytics (for beta releases)
* Fixed: Checkboxes in transaction list are hard to see
* Fixed: Crash when restoring last backup
* Improvement: Imbalance accounts are created on-the-fly (and hidden in single-entry mode)
* Improvement: Transaction auto-complete suggestions now include amount, date and do not show duplicates
* Improvement: Only one ROOT account exists in the database (it is created if necessary)
* Improvement: Show the day in transaction headers
* Improvement: Added `created_at` and `modified_at` database columns to all records
* Improvement: Added ability to mark account as favorite from context menu
* Improvement: Future transactions are not considered when calculating account balances
* Improvement: Database is always cleared during import (no merging of books supported)
* Improvement: Increased speed and reliability of import operations
* Improvement: Use Google Espresso for writing UX tests, added new test cases
* Improvement: Upgraded Java version to 1.7
* Improvement: Use gradle for building project

Version 1.5.5 *(2015-02-20)*
----------------------------
* Fixed: QIF not exported when using single-entry transactions
* Fixed: Passcode screen can be by-passed using SwipeBack
* Fixed: Crash when changing the account name

Version 1.5.4 *(2015-02-16)*
----------------------------
* Fixed: Crash when creating TRADING accounts
* Fixed: Crash when deleting scheduled transactions
* Fixed: Account parent can be set to self, creating a cyclic hierarchy
* Fixed: Transactions not saved when double-entry is enabled but no transfer account is specified
* Improved: Auto-select the device locale currency in the account-creation dialog
* Improved: Upgraded structure of repository to match latest Android conventions
* Improved: Updated instrumentation tests and enabled better test reporting with Spoon

Version 1.5.3 *(2015-02-02)*
----------------------------
* Fixed: Unable to edit double-entry transactions
* Fixed: Edited transactions not flagged unexported
* Fixed: Random crashes when editing transaction splits
* Improved: Long press on transactions triggers context menu

Version 1.5.2 *(2015-01-26)*
----------------------------
* Fixed: Crash when importing XML with TRADING accounts
* Fixed: Full name not updated when account name is changed
* Fixed: Toggle button shown when double-entry is disabled
* Fixed: Amount input problems on some devices or keyboards
* Fixed: Crash when changing the parent of an account
* Fixed: Deleting a transaction only deletes some splits, not the whole.

Version 1.5.1 *(2014-10-08)*
----------------------------
* Fixed: Crash when upgrading from v1.4.x to v1.5.x

Version 1.5.0 *(2014-10-01)*
----------------------------
* Need for speed! Lots of performance optimizations in the application
  - Application balances are now computed faster
  - App loads faster and is more responsive
  - Faster recording of opening balances before delete operations
  - Import and export operations rewritten to perform faster and use less resources
* Fixed: Crash after saving opening balances and trying to create new transactions
* Fixed: Parent account title color sometimes not propagated to child accounts
* Fixed: Recurring transactions scheduled but not saved to database during import
* Fixed: Crash caused by null exception message during import
* Fixed: Poor word-wrap of transaction type labels
* Fixed: Amount values not always displaying the correct sign
* Feature: Select default currency upon first run of application
* Feature: Creating account hierarchy uses the user currency preference
* Feature: Support for reading and writing compressed GnuCash XML files.
* Feature: Set a passcode lock to restrict access to the application
* Feature: Export a QIF file for transactions of each currency in use  
* Improved: Increased stability of import/export operations
* Improved: Exclude multi-currency transactions from QIF exports
* Improved: Display warnings/limitations of different export formats in the export dialog
* Improved: Preserve split memos in QIF export (as much as possible)
* Improved: Child accounts now assigned to account parent upon deletion of account
* Improved: Descendant accounts cannot be selected as a parent account (no cyclic dependencies)

Version 1.4.3 *(2014-09-09)*
----------------------------
* Fixed: Cannot edit transactions when in single-entry mode
* Fixed: Transaction type button sometimes hidden in single-entry mode
* Fixed: Problems saving new transactions from templates

Version 1.4.2 *(2014-08-30)*
----------------------------
* Fixed: Newly added transactions cannot be exported

Version 1.4.1 *(2014-08-25)*
----------------------------
* Fixed: Transaction edits not saved
* Fixed: Crash during import due to template transactions
* Fixed: Cursors potentially left unclosed
* Fixed: Fatal crash when error occurs in importing/exporting transaction (instead of displaying error message)
* Fixed: Editing a transfer transaction does not edit other side of the transaction
* Removed progress dialog from database migration (seems to be cause of some crashes)
* Updated German translation

Version 1.4.0 *(2014-08-15)*
----------------------------
Since transactions are at the core of this app, this release touches almost all subsystems and is therefore huge.
Transactions are now composed of multiple splits, which belong to different accounts and are no longer bound to accounts,
nor is the money amount bound to the transaction itself.
Splits store the amounts as absolute values and then separately the kind - CREDIT/DEBIT - of the split.

* Feature: Introduces multiple splits per transaction
* Feature: Introduced a new Split editor for the creation and editing of splits
* Feature: Use account specific labels for CREDIT/DEBIT instead of just generic "debit" and "credit"
* Feature: Import GnuCash XML files - accounts and transactions only (experimental)
* Feature: Back up transactions in an XML format (similar to GnuCash XML) called .gnca (Gnucash Android)
* Feature: Option for saving opening balances before deleting transactions
* Improved: Updated processes for moving, creating, exporting, deleting transactions to work with splits
* Improved: Updated computation of account and transaction balances to be in line with accounting principles
* Improved: Updated color (red/green) display to match movement in the account, and not a representation of the side of the split
* Improved: Introduced new format for sending Transactions through Intents (while maintaining backwards compatibility)
* Improved: Extensive code refactoring for
    - Better modularity of transaction exports (and ease introduction of new formats),
    - Cleaner database schema and reduction of overlap and redundancies
    - Easier database migrations during future update (with reduced risk of data loss)


Version 1.3.3 *(2014-05-26)*
----------------------------
* Reversed changes in the computation of balances, back to pre-v1.3.2 mode (will be re-instated in the future)

Version 1.3.2 *(2014-05-23)*
----------------------------
* Fixed: Editing account modifies the transaction type of transfer transactions
* Fixed: Bug causing double entry transactions not to be considered equal
* Fixed: Computation of account balance did not properly consider double entries
* Improved: Double-entry accounting is now activated by default
* Improved: Reliability of account structure import
* Improved: Restricted parent/child account hierarchies relationships to those defined by GnuCash accounting
* Improved: Dutch translation
* Improved: German translation

Version 1.3.1 *(2014-02-14)*
----------------------------
* Fixed: Crash when bulk moving transactions
* Fixed: Missing string for internationalization in ru_RU locale
* Fixed: Random crashes when opening ScheduledTransactions list
* Fixed: Blank screen after closing AccountFormFragment
* Fixed: Correct normal balance of the different types of ASSET accounts
* Fixed: Limit the target accounts for bulk transfers to same currency and non-placeholder accounts
* Improved: Remember last opened tab in accounts list
* Improved: Added version information for feedback email
* Improved: Lists of accounts are now sorted by the fully qualified account name

Version 1.3.0 *(2014-02-10)*
----------------------------
* Fixed: Some file managers do not display all files available for import
* Fixed: Crash when deleting account from accounts list
* Fixed: CASH accounts should have normal DEBIT balance
* Fixed: Crash when quickly opening and navigating from transactions list
* Feature: Mark favorite accounts and quickly access them
* Feature: Display different tabs for recent, favorite and all accounts
* Feature: Add, view and delete recurring transactions (daily, weekly, monthly)
* Feature: Mark accounts as placeholder accounts (cannot contain transactions)
* Feature: Set a default transfer account for each account
* Feature: Color code accounts & themed account views
* Feature: Create default GnuCash account structure from within app
* Improved: Enabled one-button click for rating app and sending feedback
* Improved: Clicking on version information now shows changelog
* Improved: Delete account and all its sub-accounts
* Improved: Sub-accounts default to same account type as parent account
* Improved: Use tab views for sub-accounts and transactions inside accounts

Version 1.2.7 *(2013-12-18)*
----------------------------
* Fixed: Export format always defaults to QIF, ignoring user preference
* Improved: Better responsiveness of add transaction and add account buttons
* Improved: Russian translation

Version 1.2.6 *(2013-12-06)*
----------------------------
* Feature: Support for QIF export format
* Improved: CREDIT/DEBIT meaning now matches GnuCash desktop. Effect on account balance depends on type of account

Version 1.2.5 *(2013-09-17)*
----------------------------
* Feature: Search accounts by name
* Fixed: crash when deleting accounts
* Fixed: auto-completing transaction names does not copy the time or export flag
* Fixed: random crash when opening app (or loading accounts)

Version 1.2.4 *(2013-09-05)*
----------------------------
* Added support for detecting placeholder accounts during import
* Use full qualified account names in account selection spinners
* Loads complete transaction as a template when the autocomplete suggestion is selected
* Fixed: selecting items from lists caused multiple to be selected in the wrong positions
* Fixed: widgets not updated when all accounts or all transactions are deleted
* Other minor bug fixes.

Version 1.2.3 *(2013-08-28)*
----------------------------
* Fixed: crashes when editing/creating transactions
* Feature: Added Chinese language translation
* Feature: Autocomplete transaction descriptions
* Improved reliability of importing stock accounts
* Improved speed of loading account balance
* Improved increased touch target area of "new transaction" button in accounts list view

Version 1.2.2 *(2013-06-23)*
----------------------------
* Fixed: bug with importing accounts
* Fixed: deleting an account renders sub-accounts inaccessible.
* Fixed: impossible to scroll new accounts screen
* Updated Brazilian Portuguese translations

Version 1.2.1 *(2013-06-22)*
----------------------------
* Fixed: crash when opening Settings on devices with Gingerbread or earlier
* Improved performance for loading list of accounts and transactions
* Show progress dialog for importing accounts and improve reliability

Version 1.2.0 *(2013-06-20)*
----------------------------
* Feature: Import GnuCash desktop account structure
* Feature: Nested display of account hierarchy
* Feature: Options for deleting all accounts/transactions
* Feature: Preliminary support for account types
* Fixed:   Account balance now takes sub-accounts into consideration
* Fixed:   Support for GnuCash ROOT account (will not be displayed)

Version 1.1.2 *(2013-02-03)*
----------------------------
* Fixed: Crash upon screen rotation when creating account
* Fixed: Crash when entering a transaction after deleting an account which has a widget

Version 1.1.1 *(2013-01-31)*
----------------------------
* Updated German and Norwegian (Bokmal) translations

Version 1.1.0 *(2013-01-31)*
----------------------------

* Feature: Double entry accounting - every transaction is a transfer
* Feature: Nested accounts
* Feature: SGML support for OFX exports. Exports are now SGML by default
* Feature: Display what's new to user after minor/major updates
* Improved: Reworked UX for creating accounts
* Improved: Default accounts now match GnuCash desktop accounts
* Fixed: Crash when creating accounts with special characters in the names
* Fixed: GnuCash declares itself as launcher application
* Fixed: Encoding of exported OFX not properly detected by GnuCash desktop

Version 1.0.3 *(2012-11-23)*
----------------------------

* Fixed: Crash when determining currency for en_UK which is not an ISO 3611 country
* Fixed: Crashes on Android 4.2 when editing transactions
* Improved: Better handle rotation when creating transactions
* Improved: Spanish & Italian translations

Version 1.0.2 *(2012-11-09)*
----------------------------

* Fixed: Default transaction type setting not working for non-English locales
* Added new default settings for exporting
* Improved French and German translations


Version 1.0.1 *(2012-11-05)*
----------------------------

* Feature: Select default transaction type from settings
* Feature: Navigate account transactions using action bar navigation lists
* Feature: Brazilian Portuguese translation now available
* Fixed:   Inconsistencies in some translation strings


Version 1.0.0 *(2012-11-01)*
----------------------------
Initial release.
