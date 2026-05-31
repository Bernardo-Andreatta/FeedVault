-- SecureGallery Android project local config

-- Gradle build tasks
vim.keymap.set("n", "<leader>cb", function()
  vim.cmd("botright 15split | terminal ./gradlew assembleDebug")
end, { desc = "Gradle assembleDebug", buffer = false })

vim.keymap.set("n", "<leader>ct", function()
  vim.cmd("botright 15split | terminal ./gradlew test")
end, { desc = "Gradle test", buffer = false })

vim.keymap.set("n", "<leader>ci", function()
  vim.cmd("botright 15split | terminal ./gradlew installDebug")
end, { desc = "Gradle installDebug", buffer = false })

-- Android logcat shortcut
vim.keymap.set("n", "<leader>al", function()
  vim.cmd("botright 20split | terminal adb logcat -s SecureGallery")
end, { desc = "Android logcat" })
