package org.sbtidea.android

import util.control.Exception.allCatch
import xml.NodeSeq
import sbt._
import java.io.{FileReader, File}
import org.sbtidea.{Settings, IOUtils}
import sbt.Load.BuildStructure
import org.sbtidea.Settings
import java.util.Properties

case class AndroidSupport(projectDefinition: ProjectDefinition[ProjectRef], projectRoot: File, buildStruct: BuildStructure, settings: Settings) {

  import sbtandroid.AndroidPlugin.{settings => _, _}

  def projectRelativePath(f: File) = IOUtils.relativePath(projectRoot, f, "/../")

  val isAndroidProject: Boolean = allCatch.opt {
    settings.optionalSetting(sbtandroid.AndroidPlugin.platformName).isDefined
  }.getOrElse(false)

  lazy val providedClasspath: Seq[sbt.File]=
    settings.task(sbtandroid.AndroidPlugin.providedClasspath in configuration)

  private lazy val configuration =
    settings.optionalSetting(sbtandroid.AndroidPlugin.ideaConfiguration) getOrElse Compile

  private lazy val manifest: File = settings.task(manifestPath in configuration).head

  private lazy val genFolder = projectRelativePath(setting(managedJavaPath in configuration))

  private lazy val platformVersion = {
    val props = new Properties()
    props.load(new FileReader((setting(sbtandroid.AndroidPlugin.platformPath in configuration) / "source.properties").asFile))
    props.getProperty("Platform.Version")
  }

  private lazy val proguardConfigPath =
    settings.task(proguardConfiguration in configuration) map (projectRelativePath _) getOrElse ""

  def facet: NodeSeq = {
    if (!isAndroidProject) NodeSeq.Empty
    else {
      import sbtandroid.AndroidPlugin.{settings => _, _}

      <facet type="android" name="Android">
        <configuration>
          <option name="GEN_FOLDER_RELATIVE_PATH_APT" value={ genFolder } />
          <option name="GEN_FOLDER_RELATIVE_PATH_AIDL" value={ genFolder } />
          <option name="MANIFEST_FILE_RELATIVE_PATH" value={ projectRelativePath(manifest) } />
          <option name="RES_FOLDER_RELATIVE_PATH" value={ projectRelativePath(settings.task(mainResPath in configuration)) }/>
          <option name="ASSETS_FOLDER_RELATIVE_PATH" value={ projectRelativePath(setting(mainAssetsPath in configuration)) } />
          <option name="LIBS_FOLDER_RELATIVE_PATH" value={ projectRelativePath(setting(unmanagedNativePath in configuration)) } />
          <option name="USE_CUSTOM_APK_RESOURCE_FOLDER" value="false" />
          <option name="CUSTOM_APK_RESOURCE_FOLDER" value="" />
          <option name="USE_CUSTOM_COMPILER_MANIFEST" value="false" />
          <option name="CUSTOM_COMPILER_MANIFEST" value="" />
          <option name="APK_PATH" value={ projectRelativePath(settings.task(packageApkPath in configuration)) } />
          <option name="LIBRARY_PROJECT" value="false" />
          <option name="RUN_PROCESS_RESOURCES_MAVEN_TASK" value="false" />
          <option name="GENERATE_UNSIGNED_APK" value="false" />
          <option name="CUSTOM_DEBUG_KEYSTORE_PATH" value="" />
          <option name="PACK_TEST_CODE" value="false" />
          <option name="RUN_PROGUARD" value={ setting(useProguard in configuration).toString } />
          <option name="PROGUARD_CFG_PATH" value={ proguardConfigPath.toString } />
          <resOverlayFolders>
            <path></path>
          </resOverlayFolders>
          <includeSystemProguardFile>false</includeSystemProguardFile>
          <includeAssetsFromLibraries>false</includeAssetsFromLibraries>
          <additionalNativeLibs />
        </configuration>
      </facet>
    }
  }

  def moduleJdk: NodeSeq = <orderEntry type="jdk" jdkName={"Android %s Platform".format(platformVersion)} jdkType="Android SDK" />

  private def setting[A](key: SettingKey[A]): A = settings.setting(key, "Missing setting: %s".format(key.key.label))
}
